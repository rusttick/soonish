#!/usr/bin/env bash
# PreToolUse hook for Bash: blocks commands that would get around the
# guard-rails. It is a speed bump, not a wall. The real check is the
# owner reviewing guard-rail changes, which the pre-commit hook lists.
#
# The owner can allow it for one session:  SOONISH_OWNER_EDIT=1 claude
set -euo pipefail
[ "${SOONISH_OWNER_EDIT:-}" = 1 ] && exit 0

cmd=$(jq -r '.tool_input.command // empty')

block() {
  echo "$1" >&2
  exit 2
}

case "$cmd" in
  *--update-baseline*)
    block "Only the owner raises the guard baseline. Tell them what rose and why." ;;
  *--no-verify* | *core.hooksPath*)
    block "Do not bypass the git hooks." ;;
esac

guarded='(\.claude/|bin/|guard/|\.clj-kondo/config\.edn|\.cljfmt\.edn|deps\.edn|CONTRIBUTING\.md|CLAUDE\.md|core/attrs\.clj)'
writes='(sed +-i|perl +-[a-z]*i|(^|[^[:alnum:]_-])(tee|mv|cp|rm)[[:space:]]|>[^&]|git +(checkout|restore|rm|mv|stash))'

if printf '%s' "$cmd" | grep -Eq "$guarded" &&
  printf '%s' "$cmd" | grep -Eq "$writes"; then
  block "This command looks like it changes a guard-rail file, which only the owner changes. Ask them. (To run bin/check, do not redirect its output.)"
fi
