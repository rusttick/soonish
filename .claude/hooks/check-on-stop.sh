#!/usr/bin/env bash
# Stop hook: if the sources changed since the last passing run, runs
# bin/check. When it fails, exit code 2 sends the agent back to fix it.
# If it fails a second time in a row, the agent is let go, so that it can
# report what is left instead of looping.
set -uo pipefail
input=$(cat)
cd "${CLAUDE_PROJECT_DIR:-$PWD}" || exit 0
command -v clojure > /dev/null || exit 0

stamp=.cpcache/last-passing-check
state=$(find src test examples deps.edn -type f -print0 2> /dev/null |
  sort -z | xargs -0 shasum | shasum | cut -d' ' -f1)
if [ -f "$stamp" ] && [ "$(cat "$stamp")" = "$state" ]; then
  exit 0
fi

if out=$(bin/check 2>&1); then
  mkdir -p .cpcache
  printf '%s' "$state" > "$stamp"
  exit 0
fi

if [ "$(printf '%s' "$input" | jq -r '.stop_hook_active // false')" = true ]; then
  exit 0
fi
printf 'bin/check fails. Fix it before you finish, or tell the owner what is left:\n%s\n' \
  "$(printf '%s' "$out" | tail -n 80)" >&2
exit 2
