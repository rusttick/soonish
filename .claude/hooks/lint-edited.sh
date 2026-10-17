#!/usr/bin/env bash
# PostToolUse hook for file edits: lints the edited Clojure file at once,
# so that unbalanced brackets and unknown names are fixed before anything
# is built on them. Exit code 2 shows the findings to the agent.
set -uo pipefail

path=$(jq -r '.tool_input.file_path // empty')
case "$path" in
  *.clj | *.cljc | *.edn) ;;
  *) exit 0 ;;
esac

cd "${CLAUDE_PROJECT_DIR:-$PWD}" || exit 0
if ! command -v clojure > /dev/null; then
  echo "The Clojure CLI is not installed, so the edit was not linted." >&2
  exit 1
fi

if ! out=$(clojure -M:lint --lint "$path" 2>&1); then
  printf 'clj-kondo found problems in %s. Fix them now:\n%s\n' "$path" "$out" >&2
  exit 2
fi
