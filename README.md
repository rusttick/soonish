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
rules, a ratchet, tests, git hooks and Claude Code hooks. See
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

    clojure -P -M:test:lint:fmt:guard   # download every dependency once
    bin/check                           # format, lint, guard, tests

If formatting is the only failure, run `bin/fmt`, then `bin/check`
again.

Try the program on the running example from the design:

    clojure -M -m soonish.main examples/running-example.edn

For a REPL with the tests on the classpath:

    clj -M:dev

### Git hook

Enable the pre-commit hook once per clone:

    git config core.hooksPath bin/git-hooks

It runs `bin/check` whenever a commit touches this project, and lists
any guard-rail files the commit changes. While soonish still sits
inside a larger repository, the setting applies to that whole
repository, but the hook acts only on commits that touch soonish.

### Coding agents

Start Claude Code from this directory, so that it loads `CLAUDE.md`
and the hooks in `.claude/settings.json`:

- edits to guard-rail files are blocked
- every edited Clojure file is linted at once
- `bin/check` runs when the agent stops, if sources changed

To let an agent edit guard-rail files for one session, start it with:

    SOONISH_OWNER_EDIT=1 claude

## Commands

- `bin/check`: every check. It must pass before a commit.
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

Tools outside `deps.edn`, as last checked:

- JDK 25.0.1
- Clojure CLI 1.12.6.1673

## Not done yet

- an HTTP server and storage of facts
- a Docker image, for arm64 (Raspberry Pi 5) and x86_64
- an archive of every dependency, so that any version can be rebuilt
  if an upstream source disappears
- clojure-mcp, to give agents a live REPL and bracket-aware editing

## Layout

    doc/design3.md          the design
    src/soonish/core/       the pure core
    src/soonish/main.clj    command line entry point (shell)
    test/                   tests
    examples/               example inputs
    guard/                  the project's own rules and their checker
    bin/                    check, fmt, and the git hooks
    .claude/                agent hooks and settings
    .clj-kondo/config.edn   linter settings
    .cljfmt.edn             formatter settings
