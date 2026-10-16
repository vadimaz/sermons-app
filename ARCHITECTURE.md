# Technical Architecture & Guidelines

> Single Source of Truth for AI assistants and developers working on SermonIndex Android App.

---

## 1. Tech Stack & Environment

| Component | Technology / Version | Details |
|---|---|---|
| **OS / Runtime** | Android SDK | `minSdk = 26` (Android 8.0), `compileSdk = 36`, `targetSdk = 36` |
| **JDK** | Java 17 | `jvmToolchain(17)`, Gradle 9.1.0, AGP 9.0.1 |
| **Language** | Kotlin 2.3.20 | KSP 2.3.11, Kotlin Serialization 1.8.0 |
| **UI Toolkit** | Jetpack Compose | BOM 2026.03.01, Material 3, Material Icons Extended |
| **Navigation** | Navigation 3 (`androidx.navigation3`) | Version 1.0.1 (`NavDisplay`, `NavKey`, multi-backstacks with `rememberSaveableStateHolderNavEntryDecorator`) |
| **Dependency Injection** | Dagger Hilt 2.60.1 | `androidx.hilt:hilt-navigation-compose:1.2.0`, `@AndroidEntryPoint`, `@HiltViewModel` |
| **Async / Reactive** | Coroutines 1.10.2 & Flow | `StateFlow`, `SharedFlow`, `flowOn(ioDispatcher)` |
| **Media Player** | AndroidX Media3 1.5.1 | `ExoPlayer`, `MediaSessionService`, `MediaController` |
| **Networking** | Retrofit 2.11.0 + OkHttp 4.12.0 | `kotlinx.serialization.json`, custom 404-safe interceptor |
| **Database & Search** | SQLiteOpenHelper + FTS4 | `AppDatabase.kt` with full-text search (`sermon_fts`), Room runtime/ktx |
| **Image Loading** | Coil 3.1.0 | `coil-compose`, `coil-network-okhttp` |
| **Background Work** | AndroidX WorkManager 2.10.0 | `CoroutineWorker`, Hilt `@EntryPoint` integration |

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
│   UseCases (15)  ──►  SermonRepository (Interface)      │
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

## 3. Package Structure

```
com.example.sermonindex
├── MainActivity.kt               # @AndroidEntryPoint, loads MainNavigation
├── Navigation.kt                 # NavDisplay, Scaffold, MiniPlayer, BottomBar, Multi-Backstack Navigator, Reselect Events
├── NavigationKeys.kt             # @Serializable NavKey definitions
├── SermonApp.kt                  # @HiltAndroidApp Application class
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt        # SQLiteOpenHelper + FTS4 virtual table + downloads/favorites/history
│   │   ├── dao/                  # FavoritesDao, HistoryDao, DownloadsDao
│   │   └── entities/             # FavoriteSermonEntity, HistoryEntity, DownloadedSermonEntity, FtsSermonResult
│   ├── mapper/
│   │   └── Mappers.kt            # DTO/Entity <-> Domain mapping extensions
│   ├── model/
│   │   └── SermonDtos.kt         # @Serializable network DTOs
│   ├── network/
│   │   ├── ConnectivityNetworkMonitor.kt # Android ConnectivityManager network callback
│   │   ├── Http404EmptyInterceptor.kt # Returns empty JSON for 404 verse lookups
│   │   ├── NetworkClient.kt      # OkHttp with disk cache & logging, Retrofit
│   │   └── SermonIndexApiService.kt # Endpoints under https://api.sermonindex.net/v2
│   ├── repository/
│   │   └── SermonRepositoryImpl.kt # Concrete domain repository with injected CoroutineDispatcher
│   └── sync/
│       ├── AudioDownloadWorker.kt # WorkManager streaming worker for MP3 files
│       └── CatalogSyncWorker.kt  # WorkManager CoroutineWorker for streaming gzip catalog
│
├── di/
│   ├── DatabaseModule.kt         # Provides AppDatabase, FavoritesDao, HistoryDao, DownloadsDao
│   ├── DispatchersModule.kt      # Qualifiers: @IoDispatcher, @DefaultDispatcher, @MainDispatcher
│   ├── MediaModule.kt            # Provides PlaybackManager as @Singleton
│   ├── NetworkModule.kt          # Provides OkHttpClient, Retrofit, SermonIndexApiService
│   └── RepositoryModule.kt       # Binds SermonRepository & NetworkMonitor
│
├── domain/
│   ├── model/
│   │   └── Models.kt             # Pure Kotlin domain data classes (no Android dependencies)
│   ├── repository/
│   │   ├── NetworkMonitor.kt     # Pure network connectivity contract
│   │   └── SermonRepository.kt   # Pure domain repository interface
│   └── usecase/
│       └── UseCases.kt           # 20 single-responsibility UseCase classes
│
├── playback/
│   ├── PlaybackManager.kt        # MediaController wrapper, playbackState StateFlow, sleep timer
│   ├── PlaybackService.kt        # @AndroidEntryPoint MediaSessionService + ExoPlayer + failover & local audio
│   └── PlaybackState.kt          # Immutable playback state model
│
├── theme/
│   ├── Color.kt, Theme.kt, Type.kt # Material 3 Design System
│
└── ui/
    ├── components/
    │   ├── CommonComponents.kt   # TopBar, LoadingView, EmptyStateView, ErrorView
    │   ├── MiniPlayer.kt         # Persistent floating audio bar
    │   ├── OfflineBanner.kt      # Animated real-time offline warning banner
    │   └── SermonListItem.kt     # Standard list item with avatar, title, duration
    ├── home/                     # HomeScreen.kt, HomeViewModel.kt
    ├── library/                  # LibraryScreen.kt, LibraryViewModel.kt
    ├── player/                   # FullscreenPlayerScreen.kt, PlayerViewModel.kt
    ├── scripture/                # ScriptureScreen.kt, ScriptureViewModel.kt
    ├── sermon/                   # SermonDetailScreen.kt, SermonDetailViewModel.kt
    ├── speakers/                 # SpeakersScreen.kt, SpeakerDetailScreen.kt, ViewModels
    └── topics/                   # TopicsScreen.kt, TopicDetailScreen.kt, ViewModels
```

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

---

## 5. Verification Commands

Run unit tests:
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew testDebugUnitTest
```

Build debug APK:
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
