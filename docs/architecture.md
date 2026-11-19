# Technical Architecture & Guidelines

> Single Source of Truth for AI assistants and developers working on SermonIndex Android App.  
> Complete API specification & contracts: see [`api.md`](api.md).  
> Testing standards & recipes: see [`testing.md`](testing.md).

---

## 1. Tech Stack & Environment

| Component | Technology / Version | Details |
|---|---|---|
| **OS / Runtime** | Android SDK | `minSdk = 26` (Android 8.0), `compileSdk = 37`, `targetSdk = 36`, `<profileable>` shell enabled |
| **JDK** | Java 17 | `jvmToolchain(17)`, Gradle 9.8.2, AGP 9.4.1 |
| **Language** | Kotlin 2.3.21 | KSP 2.3.12, Kotlin Serialization 1.11.0 |
| **UI Toolkit** | Jetpack Compose | BOM 2026.09.00, Material 3 Expressive (1.5.0-beta02), Material Icons Extended |
| **Navigation** | Navigation 3 (`androidx.navigation3`) | Version 1.2.0 (`NavDisplay`, `NavKey`, multi-backstacks with `rememberSaveableStateHolderNavEntryDecorator` & `rememberViewModelStoreNavEntryDecorator`) |
| **Dependency Injection** | Dagger Hilt 2.60.1 | `androidx.hilt:hilt-navigation-compose:1.4.0` (`androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`), `@AndroidEntryPoint`, `@HiltViewModel` |
| **Async / Reactive** | Coroutines 1.11.0 & Flow | `StateFlow`, `SharedFlow`, `flowOn(ioDispatcher)` |
| **Media Player** | AndroidX Media3 1.11.1 | `ExoPlayer`, `MediaSessionService`, `MediaController`, `media3-ui-compose` (`ContentFrame`) |
| **Networking** | Retrofit 3.0.0 + OkHttp 5.5.1 | `kotlinx.serialization.json`, custom 404-safe interceptor |
| **Database & Search** | Android Room 2.8.5 + FTS4 | `AppDatabase.kt` (schema version 2) with full-text search (`sermon_fts`), DAOs (`FavoritesDao`, `HistoryDao`, `DownloadsDao`, `SearchDao`, `BibleBookmarksDao`), schema export |
| **Preferences** | Jetpack DataStore 1.3.0 | Preferences DataStore (`UserPreferencesRepositoryImpl`) |
| **Image Loading** | Coil 3.6.3 | `coil-compose`, `coil-network-okhttp` |
| **Paging** | AndroidX Paging 3 (3.5.3) | `androidx.paging:paging-runtime`, `androidx.paging:paging-compose` |
| **Background Work** | AndroidX WorkManager 2.12.0 | `CoroutineWorker`, Hilt `@EntryPoint` integration |
| **Testing** | JUnit 4 + Robolectric 4.14 + Turbine 1.2.1 + JaCoCo | JVM-first testing pyramid (75% Unit / 20% Headless UI & Room / 5% E2E) in `app/src/test/`, shared state-backed Fakes & `MainDispatcherRule` in `testing/`, governed by [`docs/testing.md`](testing.md) |

---

## 2. Architectural Blueprint: Clean Architecture & Modern Android Development (MAD)

The application strictly adheres to **Clean Architecture** principles and Google's official **Modern Android Development (MAD)** practices:
- **Modern Android Development (MAD)**: 100% Kotlin, declarative UI with Jetpack Compose & Material 3, modern dependency injection with Dagger Hilt, reactive programming with Coroutines & StateFlow, official AndroidX Media3 for media playback, and Gradle Version Catalogs with KSP. Legacy components (XML Views, Fragment transactions, `LiveData`, manual Singletons) are prohibited.
- **Clean Architecture Layers**: Strict separation between Presentation, Domain, and Data layers with unidirectional data flow (MVI / MVVM).

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                   │
│   Composable UI  ◄──(StateFlow)──  @HiltViewModel       │
│                  ───(UiEvent)───►                       │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│                       Domain Layer                      │
│   UseCases (16)  ──►  SermonRepository (Interface)      │
│   Pure Domain Models (Sermon, Speaker, Topic, etc.)     │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│                        Data Layer                       │
│   SermonRepositoryImpl  ──►  Network (Retrofit/OkHttp)  │
│   Mappers (DTO <-> Domain <-> Entity)  ──►  Local DB/FTS│
└─────────────────────────────────────────────────────────┘
```

---

## 3. Multi-Module Project Structure (Google MAD / Now in Android Standard)

The project is organized into **21 Gradle modules** governed by convention plugins in `build-logic/convention`:
- **`:app`**: Thin application shell (`SermonApp`, `MainActivity`, root `Navigation.kt` with Navigation 3 `NavDisplay` & `ShortNavigationBar`).
- **`:core:*` (11 modules)**: Horizontal foundation, domain, data, media, design system, navigation, and testing layers.
  - `:core:model` *(Pure JVM)*: Domain models (`Models.kt`).
  - `:core:common`: Coroutine dispatcher qualifiers (`DispatchersModule.kt`).
  - `:core:navigation`: Navigation 3 `@Serializable NavKey` route contracts (`NavigationKeys.kt`).
  - `:core:domain` *(Pure JVM)*: Repository interfaces (`SermonRepository`, `UserPreferencesRepository`, `NetworkMonitor`) and all UseCases.
  - `:core:network`: Retrofit, OkHttp, `Http404EmptyInterceptor`, `ConnectivityNetworkMonitor`, network DTOs, `VttParser`, `NetworkModule`.
  - `:core:database`: Room `AppDatabase`, FTS4 virtual table, 5 DAOs, entities, `DatabaseModule`, Room `schemas/`.
  - `:core:datastore`: Jetpack DataStore preferences (`UserPreferencesRepositoryImpl`, `DataStoreModule`).
  - `:core:data`: `SermonRepositoryImpl`, `Mappers.kt`, `CacheManager`, WorkManager sync/download workers (`AudioDownloadWorker`, `CatalogSyncWorker`, `CatalogUpdateCheckWorker`, `CatalogUpdateBroadcastReceiver`), `RepositoryModule`.
  - `:core:media`: Media3 playback engine (`PlaybackManager`, `PlaybackService`, `PlaybackState`, `MediaModule`, `FakePlaybackManager`).
  - `:core:ui`: Material 3 Expressive theme (`Color.kt`, `Theme.kt`, `Type.kt`, `ThemePreviews.kt`), core reusable UI components (`SermonListItem`, `MiniPlayer`, `OfflineBanner`, `LoadingView`, `ErrorView`, `EmptyStateView`, `SearchTextField`, `PreviewFixtures`, `TextHighlightUtils`, `QuoteCardSheet`), `LocalPictureInPictureMode`, and centralized UI resources (`strings.xml`).
  - `:core:testing` *(Pure JVM)*: Shared test infrastructure (`MainDispatcherRule`, `FakeSermonRepository`, `FakeUserPreferencesRepository`, `TestFixtures`).
- **`:feature:*` (9 independent modules)**: Vertical feature slices with zero cross-feature dependencies:
  - `:feature:home` (`ui/home/*`)
  - `:feature:search` (`ui/search/*`)
  - `:feature:bible` (`ui/bible/*`)
  - `:feature:scripture` (`ui/scripture/*` + `ui/bible/sermons/*`)
  - `:feature:speakers` (`ui/speakers/*` + Paging 3 `SpeakerSermonsPagingSource`)
  - `:feature:topics` (`ui/topics/*`)
  - `:feature:sermon` (`ui/sermon/*` + `ui/reader/*`)
  - `:feature:player` (`ui/player/*`)
  - `:feature:library` (`ui/library/*` + `ui/settings/*`)

---

## 4. Architectural Rules & Conventions

### 4.1 Presentation Layer (MVI / MVVM)
1. **Immutable State**: Every screen has a `@Immutable data class FooUiState(...)`. All collections must be immutable `List` or `Set`.
2. **Sealed Events**: User intents are modeled as `sealed interface FooUiEvent`.
3. **State Exposure**: ViewModels expose state strictly via `StateFlow<FooUiState>` (`asStateFlow()`), and update it via `_uiState.update { ... }`.
4. **Stateful vs Stateless Composables**:
   - **Stateful Entry Point**: Default Composable injecting `viewModel: FooViewModel = hiltViewModel()` and collecting `uiState by viewModel.uiState.collectAsStateWithLifecycle()`.
   - **Stateless Content**: Inner Composable accepting `state: FooUiState` and `onEvent: (FooUiEvent) -> Unit`.
5. **No Direct I/O in UI**: No repository queries, network calls, or database operations directly inside `LaunchedEffect` in Composables. Everything flows through the ViewModel.
6. **Assisted Injection**: For screens requiring route arguments (e.g. `sermonId`, `slug`), use `@HiltViewModel(assistedFactory = ...)` and `@AssistedInject`.
7. **Paging 3 Integration**: When paginating large collections, bridge pure domain UseCases into `Flow<PagingData<T>>` via a dedicated `PagingSource` without polluting the domain layer. Collect via `collectAsLazyPagingItems()` and render append/refresh `LoadState` (loading spinners and error retry actions).
8. **Compose Previews Standards & Verification**: All standalone stateless components and screen content composables must include a private `@ThemePreviews` preview function rendering in both Light and Dark modes. Core components live in `ui/components/`, while feature-specific subcomponents reside in `ui/<feature>/components/`. Mock models are centralized in `ui/components/PreviewFixtures.kt` to avoid test fixture duplication. Previews must be wrapped in `SermonIndexTheme { Surface { ... } }`, contain zero side-effects, and be verifiable post-implementation via Android CLI (`android preview render`).

### 4.2 Domain Layer
1. **Zero External Dependencies**: `domain/model/Models.kt` and `domain/usecase/UseCases.kt` must remain pure Kotlin. Never import Room, Retrofit, Android SDK, or Compose in Domain.
2. **Single Responsibility Use Cases**: Each UseCase encapsulates one business action (`operator fun invoke(...)`).
3. **Repository Contract**: Return `Result<T>` for one-shot operations, or `Flow<T>` for reactive database streams.

### 4.3 Data Layer & Mapping
1. **Strict Mapping Separation**:
   - Network DTOs live in `data/model/` with `@Serializable`.
   - Room/SQLite Entities live in `data/local/entities/`.
   - Domain models live in `domain/model/`.
   - All conversions go through extension functions in `data/mapper/Mappers.kt`.
2. **Dispatcher Injection**: Never hardcode `Dispatchers.IO`. Always inject `@IoDispatcher private val ioDispatcher: CoroutineDispatcher`.

### 4.4 Dependency Injection (Hilt)
- Modules reside in `di/`.
- Use `@Binds` for interfaces (`RepositoryModule`).
- Service & Activity classes must be annotated with `@AndroidEntryPoint`.
- Background tasks (WorkManager) access dependencies via `@EntryPoint` (`CatalogSyncWorkerEntryPoint`).

### 4.5 Resources & Accessibility
1. **No Hardcoded Strings**: All UI text, content descriptions, and error messages MUST use `stringResource(R.string.foo)` or `context.getString(R.string.foo)`.
2. **Positional Arguments**: Dynamic strings must format with positional tokens (e.g. `%1$s`, `%1$d`).
3. **Accessibility**: Always provide meaningful `contentDescription` for `Icon`, `IconButton`, and images.

### 4.6 Versioning Strategy (SemVer)
The project adheres to **Semantic Versioning (`MAJOR.MINOR.PATCH`)**:
- Location: `app/build.gradle.kts` (`defaultConfig { versionCode, versionName }`).
- **`versionCode`**: Monotonically incremented integer (`+1` on each commit/release).
- **`versionName`**:
  - `PATCH`: bug fixes, internal refactorings, string extractions, UI polish (`fix:`, `refactor:`, `style:`).
  - `MINOR`: new user-facing features, screens, offline sync mechanisms (`feat:`).
  - `MAJOR`: fundamental architecture redesign or breaking data/schema changes.

### 4.7 Testing & Verification ([`docs/testing.md`](testing.md))
- **Fakes Over Mocks**: Prefer reusable state-backed fakes in `com.example.sermonindex.testing` over mocking frameworks or duplicated inline stubs, evolving them alongside domain contracts.
- **Flow & ViewModel Verification**: Use `runTest`, `MainDispatcherRule`, direct state assertions (`.value` / `.first()`) for single-state checks, and CashApp Turbine (`flow.test {}`) for multi-step emission sequences or cold `Flow` streams.
- **Headless UI & Screenshot Tests**: Run Compose tests on JVM via Robolectric and Roborazzi without emulators.

---

## 5. Verification Commands

Run single test class (fast iterative loop):
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew testDebugUnitTest --tests "com.example.sermonindex.path.ClassName"
```

Run full unit test suite:
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew testDebugUnitTest
```

Build debug APK:
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
