#!/usr/bin/env bash
# PreToolUse hook for file edits: agents may not change guard-rail files
# (the list is in bin/protected). Exit code 2 blocks the edit and shows
# the message to the agent.
#
# The owner can allow it for one session:  SOONISH_OWNER_EDIT=1 claude
set -euo pipefail
[ "${SOONISH_OWNER_EDIT:-}" = 1 ] && exit 0

path=$(jq -r '.tool_input.file_path // .tool_input.notebook_path // empty')
[ -n "$path" ] || exit 0

root="${CLAUDE_PROJECT_DIR:-$PWD}"
rel="${path#"$root"/}"

if "$root/bin/protected" "$rel"; then
  echo "$rel is a guard-rail file, and only the owner changes it." >&2
  echo "Stop and ask the owner. Say what you need changed and why." >&2
  exit 2
fi
