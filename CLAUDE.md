# soonish

Instructions for coding agents. The full rules are in CONTRIBUTING.md,
imported below. Follow them exactly.

- The design is `doc/design3.md`. Step 0 is the current step.
- Pure code goes in `src/soonish/core/`. Effects and state go only in
  the other namespaces, and those stay thin.
- Use the REPL (`clojure_eval`) to check what functions return
  before you rely on them. Every definition still goes in a file.
- Run `bin/check` before you finish. It must pass.
- When a check fails, fix the code. Never loosen a check, suppress a
  warning or weaken a test.
- Guard-rail files are blocked for agents. When you need one changed,
  stop and ask the owner, and say what and why.
- Ask before adding a dependency or a new attribute.

@CONTRIBUTING.md
