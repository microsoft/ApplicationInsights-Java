# AGENTS.md

Guidance for coding agents working in this local clone of `microsoft/ApplicationInsights-Java`.

## Repository Context

- This is a GitHub repository cloned locally at `C:\dev\unit\github\ApplicationInsights-Java`.
- Primary build tool: Gradle Kotlin DSL.
- Use the Gradle wrapper from the repo root: `.\gradlew.bat` on Windows, `./gradlew` on Unix-like shells.
- Main areas:
  - `agent/`: Java agent, tooling, instrumentation, profiler, GC monitor
  - `classic-sdk/`: classic SDK modules
  - `smoke-tests/`: smoke test apps and framework
  - `perf-tests/`: performance test applications
  - `etw/`: ETW-related modules
  - `buildSrc/`: shared Gradle conventions and build logic

## Java And Build Expectations

- Gradle conventions in `buildSrc` use Java toolchains with language version `21`.
- Java sources are generally compiled with `--release 8`.
- If Gradle fails because it is running on Java 11, switch the Gradle JVM to Java 17+ before retrying.
- Follow existing dependency locking and license-report workflows when changing dependencies.

## Safe Workflow

- Start by reading `README.md` and `CONTRIBUTING.md` for repository expectations.
- Prefer targeted changes in the smallest relevant module.
- Preserve existing behavior unless the task explicitly asks for a functional change.
- Do not edit generated outputs, lockfiles, or license reports unless the task requires dependency changes.
- Avoid broad refactors across unrelated modules.

## Validation

- For focused code changes, run the narrowest relevant Gradle task first, for example:
  - `.\gradlew.bat :agent:agent-profiler:agent-alerting:test`
  - `.\gradlew.bat :agent:agent-tooling:test`
- For wider changes, run the nearest enclosing module test task before considering broader verification.
- If dependency versions change, also run:
  - `.\gradlew.bat resolveAndLockAll --write-locks`
  - `.\gradlew.bat generateLicenseReport --no-build-cache`

## Coding Notes

- Match the existing style and module boundaries.
- Keep comments sparse and only where they clarify non-obvious logic.
- Prefer behavior-preserving optimizations and focused fixes over speculative cleanup.
- Check for existing tests before adding new ones; add tests when behavior or edge cases change.

## Output Expectations

- Summarize what changed, which module was touched, and what validation was run.
- If validation could not be completed, state the exact blocker, such as missing Java 17+/21 for Gradle.
