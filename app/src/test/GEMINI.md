# Testing Recommendations (`app/src/test`)

General guidelines for writing and maintaining tests as the project evolves:

1. **JVM-First & Self-Verification:**
   - Default to fast local JVM unit tests (`src/test/`).
   - Run targeted tests (`./gradlew testDebugUnitTest --tests "..."`) for quick feedback during development, and run the full test suite before completing a task.
   - Diagnose failing tests from stack traces and fix root causes rather than weakening assertions or adding arbitrary delays.

2. **State-Backed Fakes Over Mocks:**
   - Prefer reusable, state-backed `Fake*` implementations in the shared `testing` package over mocking frameworks or duplicated inline stubs.
   - Evolve existing fakes or add new ones naturally as domain interfaces and platform managers grow.

3. **Coroutines, Flows & ViewModels:**
   - Use `runTest` and `MainDispatcherRule` for coroutine and ViewModel tests.
   - Verify ViewModels by dispatching UI events and asserting resulting state updates — use simple state checks (`.value` / `.first()`) for single-state assertions, and **Turbine** (`flow.test {}`) for multi-step emission sequences or cold `Flow` streams.

4. **UI, Database & Network (On-Demand):**
   - Introduce UI, DAO, or HTTP tests pragmatically when needed without adding heavy test dependencies prematurely.
   - When testing Compose UI, prefer stateless `@Composable` functions and semantic/accessibility matchers over test tags.
