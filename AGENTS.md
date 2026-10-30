# SermonIndex Android — Agent Working Guidelines

> Single Entry Point & Operational Guidelines for AI Assistants & Developers.

---

## 1. Session Initialization (Mandatory)
At the start of **any new session or task**, the agent must:
1. Consult [`docs/architecture.md`](docs/architecture.md) to align with the technical stack, package structure, and strict architectural rules (Clean Architecture, MVI/MVVM, Dagger Hilt, Jetpack Compose, Media3).
2. Consult [`docs/progress.md`](docs/progress.md) to understand the current implementation status, available API contracts, and roadmap priorities.
3. Consult [`docs/testing.md`](docs/testing.md) whenever creating, updating, or reviewing tests.
4. Consult [`docs/api.md`](docs/api.md) for REST API endpoints, DTO models, and edge CDN distribution rules.

---

## 2. Core Development Rules
- **Clean Architecture & Modern Android Development (MAD)**:
  - Strictly follow Clean Architecture (Presentation -> Domain -> Data) and Google's official **Modern Android Development (MAD)** standards: Kotlin-first, declarative Jetpack Compose (Material 3), Navigation 3, Dagger Hilt, Coroutines & Flow, and AndroidX Media3.
  - Legacy Android patterns (XML layouts, `findViewById`, Fragment transactions, `LiveData`, manual Service Locators, legacy MediaPlayer) are **strictly forbidden**.
- **No Hardcoded Strings**: All UI text, content descriptions, and error messages must use `stringResource(R.string.foo)` or `context.getString(R.string.foo)`.
- **Pure Domain**: Code in `domain/` must remain pure Kotlin with zero Android/Room/Retrofit/Compose dependencies.
- **MVI / MVVM State Flow**:
  - Expose `@Immutable data class FooUiState` via `StateFlow`.
  - Handle user actions via `sealed interface FooUiEvent`.
  - Never trigger network or repository queries directly inside Composable `LaunchedEffect`.
- **Dependency Injection**:
  - Always inject `@IoDispatcher CoroutineDispatcher` instead of hardcoding `Dispatchers.IO`.
  - Use `@HiltViewModel` for ViewModels (or `@AssistedInject` with `@AssistedFactory` for routes with arguments).
- **Insets & Layout**:
  - Respect Scaffold insets and prevent double padding (`statusBarsPadding`, `navigationBarsPadding`).

---

## 3. Git Commit & Documentation Synchronization
When the user gives a **direct command to commit** (e.g., *"Сделай коммит"*, *"Закоммить и запушь"*):
1. **Bump Version (`app/build.gradle.kts`)**:
   - Increment `versionCode` by `+1`.
   - Update `versionName` following **Semantic Versioning (`MAJOR.MINOR.PATCH`)**:
     * `PATCH` (e.g. `1.0.0` -> `1.0.1`): Bug fixes, refactoring, performance/string improvements (`fix:`, `refactor:`, `style:`, `perf:`).
     * `MINOR` (e.g. `1.0.1` -> `1.1.0`): New feature, new screen, new download manager (`feat:`).
     * `MAJOR` (e.g. `1.x.x` -> `2.0.0`): Breaking architectural shift, backward-incompatible DB/API changes.
2. **Check & Update [`docs/progress.md`](docs/progress.md)**:
   - Update current version and `versionCode` in the document header.
   - Move completed features/screens from *In Progress / Next Steps* to *Implemented Features*.
   - Add any newly introduced public interfaces, repository methods, or services to *API Contracts*.
   - Record any known trade-offs, deferred items, or technical debt into *Backlog & Tech Debt*.
   - Update the *Next Steps* section with the upcoming priority.
3. **Check & Update [`docs/architecture.md`](docs/architecture.md)** *(only if applicable)*:
   - Update if new foundational dependencies, architectural patterns, top-level packages, or coding guidelines were introduced.
4. **Stage, Commit & Push**:
   - Stage code changes together with the updated documentation and build files.
   - Create a conventional commit message (`feat:`, `fix:`, `refactor:`, `docs:`, etc.) accurately describing the changes.
   - Push to remote only when explicitly requested by the user.

---

## 4. Verification & Testing
- **Unit & Robolectric Tests:** Run `./gradlew testDebugUnitTest` (or `--tests "..."` for targeted checks) before completing tasks:
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
  ./gradlew testDebugUnitTest
  ```
- **Testing Architecture & Code Recipes:** Always consult [`docs/testing.md`](docs/testing.md) for testing guidelines:
  - Prefer shared state-backed Fakes in `com.example.sermonindex.testing` over mocks.
  - Use `runTest` and `Turbine` (`flow.test {}`) for coroutines and StateFlow.
  - Use Robolectric + Compose testing APIs for headless UI verification on JVM.
- **UI Verification & Visual Delivery (Compose Previews):**
  - When completing any task that creates or modifies UI screens or components:
    1. Ensure stateless composables have a private `@ThemePreviews` preview function using `PreviewFixtures`.
    2. Render previews via Android CLI (`android preview render` or Compose Preview sidecar).
    3. Present the rendered visual snapshots (Light & Dark themes via chat carousel or image embeds) to the user upon plan completion for visual review and potential follow-up adjustments before committing.
- **Live Device Checks (`android` CLI):** Inspect layout hierarchy (`android layout`) before capturing screenshots, and store temporary captures in `build/tmp/`.

---

## 5. Planning & Language Conventions
- **Implementation Plans & Planning Artifacts**:
  - All implementation plans, task breakdowns, architectural drafts, and technical specifications must be written **strictly in English** (ensuring technical clarity, precise symbol referencing, and standard engineering terminology).
  - For any task affecting UI screens or components, always include a dedicated final verification step: *"Visual Verification: Render Compose Previews (Light/Dark mode) & present snapshots to user"*.
  - Store plans and persistent artifacts in the designated artifact directory or markdown documentation files.
- **Chat Communication & Plan Summaries**:
  - Conversational replies, discussions, and explanations in the chat must be conducted in the user's preferred language (Russian by default).
  - When introducing or updating an implementation plan, provide a **concise summary in Russian in the chat** highlighting:
    1. Key phases / milestones.
    2. Critical architectural decisions or risks.
    3. Decisions or open questions requiring user confirmation.
    4. A direct reference/link to the full English implementation plan artifact.
