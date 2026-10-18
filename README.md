# soonish

The server for a project estimation app built on probability. It
starts as a plain network scheduler, records every estimate and
outcome, and earns each later step through evidence that it forecasts
better. The design is in [doc/design3.md](doc/design3.md).

The client is a separate project, **suunishi**.

## Status

Step 0 has begun. The core computes the forward pass of the critical
path method, with recorded actual dates replacing computed ones. There
is no backward pass, no storage and no HTTP server yet.

The guard-rails are in place: formatting, linting, the project's own
rules, a ratchet, tests and Claude Code hooks. See
[CONTRIBUTING.md](CONTRIBUTING.md).

## Development setup

You need these on your PATH:

- **a JDK, version 25** (the current long-term support release). On
  macOS with Homebrew:

      brew install openjdk
      sudo ln -sfn /opt/homebrew/opt/openjdk/libexec/openjdk.jdk \
        /Library/Java/JavaVirtualMachines/openjdk.jdk

  The link lets macOS tools find it. Any JDK 25 build works, for
  example Eclipse Temurin.
- **the Clojure CLI**, version 1.12.6 or later. On macOS:

      brew install clojure/tools/clojure

  On Linux, follow the "Linux instructions" at
  https://clojure.org/guides/install_clojure
- **git** and **jq**. Both are installed by default on macOS 15 and
  later. On Linux, use the package manager.

Check them:

    java -version          # openjdk version "25..."
    clojure --version      # Clojure CLI version 1.12.6...
    jq --version

Then, from this directory:

    clojure -P -M:test:lint:fmt:guard:nrepl   # download dependencies
    clojure -P -T:mcp                         # and the agents' REPL tool
    bin/check                                 # format, lint, guard, tests

If formatting is the only failure, run `bin/fmt`, then `bin/check`
again.

Try the program on the running example from the design:

    clojure -M -m soonish.main examples/running-example.edn

For a REPL with the tests on the classpath:

    clj -M:dev

### Tests

Run everything before each commit:

    bin/check

It runs four checks in order, and reports at the end which failed:

1. **format:** `clojure -M:fmt check src test guard/src`. Fix
   failures with `bin/fmt`.
2. **lint:** `clojure -M:lint --lint src test guard/src`
3. **guard:** `clojure -M:guard`. The project's own rules: bans,
   the attribute registry, unused public vars, and the ratchet.
4. **test:** `clojure -M:test`. Every test in `test/`.

Each line above also runs alone.

The tests so far, all in `test/soonish/core/schedule_test.clj`:

- `running-example-dates`: the running example of the design gives
  A 0–3, B 3–8, C 3–7, D 8–10 and E 10–13.
- `actual-dates-replace-computed-ones`: recorded actual starts and
  finishes move the later tasks.
- `problems-are-described-as-data`: unknown tasks and cycles come back
  as data, not exceptions.
- `the-order-puts-each-task-after-what-it-waits-for`: a property
  test over 200 generated task networks.
- `each-task-starts-after-what-it-waits-for`: a property test over
  200 generated task networks.

To run one namespace, or one test:

    clojure -M:test -n soonish.core.schedule-test
    clojure -M:test -v soonish.core.schedule-test/running-example-dates

A failing property test prints its `:seed`. To rerun it with that
seed, or with more cases, call the test as a function in a REPL
(`clj -M:dev`):

    (require 'soonish.core.schedule-test)
    (soonish.core.schedule-test/each-task-starts-after-what-it-waits-for
     1000 :seed 1792280945613)

To run the program on the running example:

    clojure -M -m soonish.main examples/running-example.edn

It prints each task's `:schedule/start-day` and `:schedule/finish-day`.
The last task, E, finishes on day 13.

### Coding agents

Start Claude Code from this directory, so that it loads `CLAUDE.md`,
`.mcp.json` and the hooks in `.claude/settings.json`:

- edits to guard-rail files are blocked, by file tools, Bash or the
  REPL
- every edited Clojure file is linted at once
- `bin/check` runs when the agent stops, if sources changed
- the agent gets a live REPL through
  [clojure-mcp](https://github.com/bhauman/clojure-mcp), with its
  `:repl` profile: `clojure_eval`, `list_nrepl_ports` and
  `paren_repair`. The agent keeps Claude Code's own file tools.

clojure-mcp is pinned in `deps.edn` (alias `:mcp`), so nothing is
installed globally. When Claude Code starts it, it starts its own
nREPL with `clojure -M:nrepl` and stops it on exit. The first start
downloads its dependencies, which takes a minute.

Check the MCP server with `/mcp` inside Claude Code. To check it by
hand:

    clojure -T:mcp start :config-profile :repl \
      :start-nrepl-cmd '["clojure" "-M:nrepl"]'

It prints JSON-RPC lines and waits; stop it with Ctrl-C.

Each `clojure_eval` asks for permission, because it runs arbitrary
code. To allow it without asking, add
`"mcp__clojure-mcp__clojure_eval"` to `permissions.allow` in
`.claude/settings.local.json`, which is not committed.

To connect your editor to the same REPL as the agent, use the port in
`.nrepl-port`. To run a REPL of your own instead:

    clojure -M:nrepl

To let an agent edit guard-rail files for one session, start it with:

    SOONISH_OWNER_EDIT=1 claude

## Commands

- `bin/check`: every check. Run it before each commit.
- `bin/fmt`: reformat the sources.
- `clojure -M:test`: the tests only.
- `clojure -M:lint --lint src test guard/src`: clj-kondo only.
- `clojure -M:guard`: the project's own rules only.
- `clojure -M:guard --update-baseline`: accept new ratchet counts.
  Owner only.

## Pinned versions

All versions are exact, in `deps.edn`:

- Clojure 1.12.6
- clj-kondo 2026.08.04
- cljfmt 0.16.6
- test.check 1.1.3
- cognitect test-runner v0.5.1 (git sha dfb30dd)
- nREPL 1.8.0
- clojure-mcp v0.6.0 (git sha 2b0213f)

Tools outside `deps.edn`, as last checked:

- JDK 25.0.1
- Clojure CLI 1.12.6.1673

## Not done yet

- an HTTP server and storage of facts
- a Docker image, for arm64 (Raspberry Pi 5) and x86_64
- an archive of every dependency, so that any version can be rebuilt
  if an upstream source disappears

## Layout

    doc/design3.md          the design
    src/soonish/core/       the pure core
    src/soonish/main.clj    command line entry point (shell)
    test/                   tests
    examples/               example inputs
    guard/                  the project's own rules and their checker
    bin/                    check, fmt, and the guard-rail file list
    .claude/                agent hooks and settings
    .mcp.json               agent tools: clojure-mcp
    .clj-kondo/config.edn   linter settings
    .cljfmt.edn             formatter settings
