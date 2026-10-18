# Contributing to soonish

These rules are for everyone who writes code here, and above all for
coding agents. They follow Rich Hickey's approach to software: values
instead of objects, pure functions instead of methods, and explicit,
minimal state. Clojure was designed for this way of working, so most
of the rules describe ordinary, idiomatic Clojure. The rules exist to
keep it that way as the code grows.

Each rule ends with how it is checked:

- **guard**: the project's own checker, rules in `guard/rules.edn`
- **kondo**: the clj-kondo linter, settings in `.clj-kondo/config.edn`
- **fmt**: the cljfmt formatter
- **tests**: the test suite
- **review**: nobody but a reader. These rules are the ones that slip,
  so look for them first when you review.

## The test

Before adding anything, ask: **what does this entangle?** Prefer more
separate parts to fewer knotted ones. Familiar is not the same as
simple.

## The working loop

1. Read `doc/design3.md` for what the program is meant to do. Step 0
   is the current step.
2. Write a pure function in a core namespace, and a test that calls it
   with plain values.
3. Run `bin/check`. It runs, in order: format, lint, guard, tests. It
   must pass before you finish. If you work as a Claude Code agent,
   the hooks lint each file you edit and run `bin/check` when you stop.
4. If formatting fails, run `bin/fmt`.

When a check fails, fix the code. Never loosen the check, suppress the
warning, skip the test or weaken an assertion. If a check seems wrong,
stop and tell the owner why.

## The REPL

Agents get a live REPL through clojure-mcp. Its `clojure_eval` tool
evaluates code in an nREPL running this project, with `src` and `test`
on the classpath. Use it the way a Clojure programmer would:

- **Check before you write.** Before using a function, evaluate it on
  a small value to see what it really returns. Use `(doc f)` and
  `(source f)` instead of guessing an API.
- **Load your changes.** After editing a file, evaluate
  `(require 'the.namespace :reload)` before calling into it.
- **Reproduce with values.** When something fails, capture the input
  value and call the function with it in the REPL.
- **Run one test namespace** with
  `(clojure.test/run-tests 'soonish.core.schedule-test)`.
- **Files are the truth.** Anything you define only in the REPL is
  lost, and nobody can review it. Put every definition in a file.
  `bin/check` runs outside the REPL, and it decides.
- **No effects.** Use the REPL to compute, not to write files or
  start processes. A hook blocks evaluations that look like they
  change guard-rail files.
- **Brackets.** If an edit leaves brackets you cannot balance in one
  or two tries, use the `paren_repair` tool on the file. Do not loop
  on manual fixes.

## Guard-rail files

Only the owner changes these, and agent hooks block edits to them:

- `deps.edn`: ask before adding any dependency
- `src/soonish/core/attrs.clj`: the attribute registry, which is the
  data model
- `guard/`, `.clj-kondo/config.edn`, `.cljfmt.edn`: the checks
- `bin/`, `.claude/`, `.mcp.json`: the scripts, hooks and agent
  tools
- `CONTRIBUTING.md` and `CLAUDE.md`: these rules

If your work needs a change to one of them, stop and ask, and say
exactly what you need and why.

## Layout

- `src/soonish/core/`: the pure core. Values in, values out. No
  state, no I/O, no clock, no randomness, no Java. **guard**
- everything else in `src/`: the shell. It gathers inputs, calls the
  core, and carries out effects. Keep it thin.
- `src/soonish/core/attrs.clj`: every attribute, defined once.
- `test/`: tests, mirroring `src/`.
- `examples/`: example inputs as EDN.
- `doc/design3.md`: the design. The code follows it, and it says what
  each step adds and why.

The core may require only other core namespaces and `clojure.edn`,
`clojure.math`, `clojure.set`, `clojure.string` and `clojure.walk`.
**guard**

## 1. Data

- Represent information as plain maps, sets, vectors and scalars.
  Never define a record or a type per entity: no `defrecord`,
  `deftype` or `definterface`. **guard**
- Keys are qualified keywords with a meaning of their own, such as
  `:task/estimate-days`. The meaning holds in any map that holds the
  key. **review**
- Every qualified keyword must be in the attribute registry,
  `soonish.core.attrs/registry`, with a `:doc` and a `:schema`. A
  misspelt key in Clojure returns `nil` silently. The registry makes
  it a failure instead. **guard**
- Write keywords in full. Never use auto-resolved keywords (`::k`).
  They tie the key's name to a namespace's name, and they change if the
  namespace is renamed. **guard**
- Unqualified keys are only for maps that never leave one namespace,
  such as the working state inside a function. **review**
- Leave unknown information out. Never store `nil` as a value, and
  never add a key with a placeholder. A missing key means "not known".
  **review**
- Read only the keys you need, and pass the rest through untouched.
  Use `assoc` and `update` on the map you were given. Do not rebuild
  it with `select-keys` or a new literal. **review**
- Name the keys you need where you need them: destructure with
  `{:keys [...]}` or `{:task/keys [...]}` in the function that uses
  them. Do not destructure one structure in many places. **review**
- Use named keys, not positions. If a vector or an argument list
  passes about four items, use a map. **review**

```clojure
;; yes: reads one key, keeps the rest
(defn delay-task [task days]
  (update task :task/estimate-days + days))

;; no: a type per entity, and a rebuilt map that drops the other keys
(defrecord Task [id estimate after])
(defn delay-task [t days]
  (->Task (:id t) (+ (:estimate t) days) (:after t)))
```

## 2. Functions

- Core functions are pure: the same arguments always give the same
  result, and nothing outside is read or changed. **guard**, for the
  forms it can see; **review** for the rest
- Pass in everything a function depends on, including the time, random
  numbers, ids and configuration. The core never calls `rand`,
  `random-uuid` or the clock. **guard**
- Return new values. Never change an argument. Clojure's collections
  make this the default; do not work around them with Java
  collections. **guard**
- Keep functions small, and their inputs generic. Prefer many small
  functions over a few data shapes. Small forms also keep brackets easy
  to balance. **review**
- Give every public function a docstring that says what it returns and
  what it requires of its arguments. **kondo**
- Delete dead code. A public var in `src` that nothing in `src` or
  `test` uses fails the check. Entry points that are used from outside
  are listed in `guard/rules.edn`. Make a helper private with `defn-`
  when it is not part of what the namespace offers; clj-kondo then
  reports it when it falls out of use. **guard**, **kondo**
- Describe problems as data. Return `{:schedule/cycle #{...}}` instead
  of throwing, when the caller can do something about it. Throw
  `ex-info` only for programming errors. **review**

## 3. State

- Assume you do not need state. Most programs need very little.
  **review**
- When you do, hold it in one `atom` in a shell namespace, containing
  an immutable value. The core never touches atoms, refs, agents,
  volatiles or dynamic bindings. **guard**
- Change state only with `swap!` and a pure function of the current
  value. **review**
- Keep the number of references small, at the edges of the program.
  The ratchet counts them. **guard**
- Dereference once, then pass the value down. A function that reads
  an atom is not pure; take the value as an argument instead.
  **review**
- To detect change, compare two values. **review**

## 4. Effects

- Structure every entry point as: gather the inputs, compute with pure
  functions, carry out the effects. Keep the middle large and the ends
  thin. `soonish.main` shows the shape. **review**, and the guard
  prints the sizes of core and shell
- Let pure code return a description of what should happen, as data,
  and let the shell carry it out. **review**
- Never put effects inside a lazy sequence. `(map save! xs)` does
  nothing until something reads it, and then it runs at a surprising
  time. In the shell, use `run!` or `doseq` for effects. In the core,
  `doseq`, `dotimes` and `run!` are banned, because the core has no
  effects. **guard** for core; **review** for the shell
- Do not hide I/O or logging that matters behind a function that looks
  pure. Printing and `tap>` are banned in the core. **guard**

## 5. Composition

- Describe transformations: `map`, `filter`, `reduce`, `into`,
  `group-by`, `for`, and threading with `->` and `->>`. Avoid
  `loop`/`recur` and hand-built accumulators; when an algorithm is
  iterative, a step function with `iterate` usually reads better. The
  ratchet counts `loop`. **guard**
- Write transformations independently of where their data comes from
  and where it goes. **review**
- Do not rely on the order of a map or a set. Sort when order matters,
  so that the same input always gives the same output. **review**
- Take collaborators as arguments; do not construct or look them up
  inside a function. **review**
- Keep transforming, moving, routing and remembering in separate
  parts. **review**
- Put policy and business rules in one place, as data where possible,
  instead of conditionals spread over files. **review**
- For behaviour that varies by kind of data, use a multimethod or a
  protocol, which can be extended without editing existing code. Do
  not repeat `case` or `cond` on the same kind in several places.
  **review**
- Never call Java from the core. Clojure has a function for nearly
  everything the core needs; for mathematics, use `clojure.math`.
  **guard**

## 6. Facts, storage and names

- Record facts with their time and author. Append; never update in
  place. A correction is a new fact. **review**, until there is
  storage code to check
- Keep history unless there is a specific reason to discard it.
  **review**
- Treat a database read as obtaining a value, and write queries as
  functions of that value. **review**
- Send values across every boundary: threads, processes, files, HTTP.
  **review**
- Grow by accretion: provide more, require less. Never remove or
  repurpose a public function or an attribute. If a meaning changes,
  add a new name beside the old one. **review**

## 7. Testing and debugging

- Test pure functions with plain, made-up values. If a test needs
  elaborate setup, the code under test has too much state or too many
  hidden dependencies; extract the pure part and test that. **review**
- Never use `with-redefs` or mocks. They exist to replace hidden
  dependencies, and the core has none. **guard**
- Add a property test with `test.check` where a rule holds for every
  input, as in `schedule_test.clj`. **review**
- When debugging, capture the input values and call the function with
  them at the REPL. If you cannot, note what prevented it and fix
  that. **review**
- Tests and lint are safety nets. They do not replace being able to
  reason about the code. **review**

## 8. Clojure traps for coding agents

- **Brackets.** An edit that loses a `)` breaks the file. Keep forms
  small, and edit whole top-level forms rather than fragments. After
  every edit, the clj-kondo hook reports unbalanced brackets; fix them
  before doing anything else. **kondo**
- **Silent nil.** A missing or misspelt key gives `nil`, and the error
  shows up far away. The registry catches misspelt qualified keys.
  Use the attribute names exactly as the registry has them. **guard**
- **Laziness.** See section 4. Also, do not hold the head of a large
  lazy sequence, and do not depend on when a lazy sequence is
  realised. **review**
- **Invented functions.** Use only functions you can see in
  `clojure.core`, the namespaces listed above, or code in this
  project. clj-kondo reports unknown names and wrong arities. Do not
  guess library APIs; ask the owner instead. **kondo**
- **Shadowing.** Do not name locals after `clojure.core` functions,
  such as `name`, `key`, `val`, `count`, `set` or `ns`. **kondo**
- **Build tool.** This project uses the Clojure CLI and `deps.edn`
  only. Never add Leiningen files or `project.clj`. **review**
- **Dependencies.** Prefer `clojure.core` and the standard library.
  Ask the owner before adding a library. **guard**: `deps.edn` is
  protected
- **Requires.** Require with `:as` aliases. No `:use` and no
  `:refer :all`. Keep the requires sorted. **kondo**, **fmt**

## Red flags in review

Each line pairs what you see with what to do instead.

- A setter, or a field assigned after creation: return a new value.
- Different results for the same arguments: pass the varying thing in.
- An argument changed by the callee: return the new value.
- A defensive copy: the data is already immutable; remove the copy.
- A lock taken in order to read: read an immutable value.
- A mutable Java object shared or put on a queue: send a value.
- A record or type whose only job is to hold fields: use a map.
- A long positional argument list or a wide vector: use named keys.
- Every key treated as optional: omit what is unknown, and state the
  requirements per use in the docstring.
- The same structure destructured in many places: read by key where
  needed.
- One step directly calling the next step of a pipeline: put a queue
  of values between them.
- A loop building a result in an accumulator: use a transformation.
- The same rule's conditionals in several files: one rule table.
- A stored record updated in place: append a new fact.
- A renamed or removed public function or attribute: add the new one
  beside the old one.
- A test with heavy setup: extract the pure core and test that.

## When you must deviate

Sometimes a rule must give way:

- **performance:** measure first, then mutate locally behind a pure
  interface, for example with a transient, and let only an immutable
  value escape
- **a Java library that needs mutable objects:** confine it to a
  shell adapter and convert to values at once
- **machine-like work** such as caches and connection handling:
  isolate it in a small, named shell component with a value-based
  interface

Declare the deviation on the definition, with the reason:

```clojure
(defn ^{:deviation "measured: 40x faster with a transient on 1e6 tasks"}
  index-tasks [tasks]
  (persistent! (reduce (fn [m t] (assoc! m (:task/id t) t))
                       (transient {})
                       tasks)))
```

The guard then accepts the banned names inside that definition, and
counts it. The count may not rise without the owner's agreement, so
explain each new deviation to the owner. Keep each one as small as
possible.

`:task/id` in this example is only an illustration and is not in the
registry.
