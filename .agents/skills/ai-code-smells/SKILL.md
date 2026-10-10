---
name: ai-code-smells
description: Find and fix telltale AI-generated code smells (redundant comments, swallowed errors, needless guards/wrappers, generic names, copy-pasted boilerplate, dead async, redundant boolean comparisons, placeholder TODOs, stray debug prints). Use when asked to clean up code smells, review AI-authored code, or tidy a diff/file for these specific issues.
---

# AI Code Smells

A find-then-fix workflow for the specific low-value patterns that show up
disproportionately in AI-generated code. This skill is scoped to *source code*
smells only — it does not cover UI/design smells (see `ai-design-smells` if
that exists) and it does not do general refactoring, style enforcement, or
architecture review.

## Guardrails (read before starting)

- **Do not invent findings.** Every reported item must be a real instance you
  can point to with a file:line and a quote. If a category has zero
  instances, say "none found" for it — do not stretch borderline cases to
  fill a category.
- **Do not redesign working code.** Fixes must be minimal and behavior
  preserving. Do not introduce new abstractions, helper classes, config
  flags, or "while I'm here" cleanups beyond the specific smell being fixed.
- **Do not touch code that isn't actually smelly.** A comment that explains a
  non-obvious *why* (a workaround, a subtle invariant, a hidden constraint)
  is not a smell — leave it. A guard clause that protects a genuinely
  reachable null/edge case is not a smell — leave it.
- When unsure whether something qualifies, prefer under-reporting over
  over-reporting. False positives cost more trust than a missed instance.
- Preserve existing formatting/style conventions of the file being edited.

## Phase 1 — Find

Search the target scope (a diff, a directory, or the whole repo — confirm
scope with whatever the invoking task specified) for each category below.
Use grep/ripgrep for the mechanical patterns and read surrounding context to
confirm each hit is real before listing it.

For each category, report every confirmed instance as:

```
<category>
- path/to/file.kt:123 — "<short quote of the offending line(s)>"
```

If a category has no confirmed instances, write exactly: `<category>: none found`.

### Categories

1. **Restating comments** — a comment that just repeats what the line right
   below it already says in code (e.g. `// increment counter` above
   `counter++`).
2. **Decorative section-divider comments** — banner-style comments like
   `// ══════ Section ══════`, `// ==== Something ====`, `/* ---- X ---- */`
   that exist only to visually break up a file rather than convey
   information.
3. **Narrative walkthrough comments** — "first we do X, then we do Y, finally
   Z" comments that narrate control flow step-by-step instead of explaining
   a non-obvious reason.
4. **Boilerplate doc-comments** — JSDoc/KDoc/docstrings on a trivial
   function that just restate the function name and parameter names with no
   real information (no non-obvious contract, no units, no edge case notes).
5. **Stale comments** — comments describing behavior, parameters, or return
   values the code no longer actually has (check the comment against the
   current implementation, not just its presence).
6. **Log-and-swallow catch blocks** — `catch` blocks that only log the
   error and otherwise silently continue, with no user feedback, fallback
   value, or rethrow, when that silence would hide a real failure.
7. **Log-and-rethrow-unchanged catch blocks** — `catch` blocks that log then
   immediately rethrow the exact same exception unchanged, adding nothing
   over letting it propagate.
8. **Unreachable defensive guards** — null checks / early returns / `?:`
   guards for a state that cannot actually occur given the real callers
   (verify by checking callers/types before flagging — this is the
   easiest category to get wrong).
9. **Needless single-use wrapper functions** — a function that exists only
   to call one other function with no added logic, used in exactly one
   place.
10. **Generic variable names** — `data`, `result`, `temp`, `item`, `obj`,
    `thing`, etc. used where a specific domain name is obvious from context.
11. **Copy-pasted boilerplate helpers** — the same non-trivial helper
    logic duplicated across multiple files instead of being shared/imported
    from one place.
12. **Unnecessary async/await (or Kotlin coroutine suspend) with no actual
    asynchronous work** — a `suspend fun` / `async {}` wrapper whose body is
    fully synchronous with no I/O, coroutine, or await inside.
13. **Redundant boolean comparisons** — `== true`, `== false`, `=== true`,
    `!= false`, etc. on already-boolean expressions.
14. **Placeholder TODO/FIXME with zero context** — a bare `// TODO` or
    `// FIXME` with no explanation of what remains, why, or a ticket
    reference.
15. **Leftover debug prints** — `println`/`console.log`/`print`-style debug
    output left in non-test code, not gated behind a debug/verbose flag or
    proper logger.

Present the full Phase 1 report to the user before making any changes.

## Phase 2 — Fix

Only after Phase 1 findings are reported:

- Apply a fix for each **confirmed** instance only. If the user narrows the
  list (e.g. "skip #8, those guards are real"), respect that.
- Keep each fix minimal and local:
  - Restating/decorative/narrative/stale/boilerplate comments → delete the
    comment (or correct it if partially stale and worth keeping the real
    info); don't add a replacement comment unless something non-obvious is
    genuinely being lost.
  - Log-and-swallow → add real handling appropriate to the call site
    (propagate, return a meaningful failure value, or surface to the user) —
    don't just add another log line.
  - Log-and-rethrow-unchanged → remove the redundant catch entirely and let
    the exception propagate, unless removing it would drop useful
    request-scoped context (in which case leave it, it wasn't actually a
    smell).
  - Unreachable guards → remove, only after confirming via callers/types
    that the state truly can't occur.
  - Single-use wrappers → inline the call at the one call site, remove the
    wrapper.
  - Generic names → rename to the specific domain term, update all
    references in that scope.
  - Duplicated helpers → consolidate into the existing shared location if
    one already exists nearby; do not invent a new shared module/package
    structure for this.
  - Dead async/suspend → remove the unnecessary `suspend`/`async` wrapper
    and update call sites, only if none of the call sites actually rely on
    it being asynchronous.
  - Redundant boolean comparisons → simplify to the bare expression
    (negate with `!` where needed).
  - Placeholder TODO/FIXME → only remove/resolve if the fix is actually
    doable now; otherwise leave as-is (a vague TODO isn't yours to
    silently delete unless asked).
  - Debug prints → remove, or convert to the project's real logger if the
    message has ongoing value.

## Phase 3 — Verify

Run this project's build/lint/test commands (e.g. `./gradlew compileKotlin`,
`./gradlew ktlintCheck`, `./gradlew test` for this repo) after fixes and
report pass/fail. Fix any breakage the edits caused before declaring done.
