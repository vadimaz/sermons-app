# SermonIndex Android — Testing Strategy & Guidelines

> Single Source of Truth for Testing Standards, Test Doubles, and Quality Assurance.

---

## 1. Testing Pyramid (MAD Standard 2026)

The project adheres to a fast, reliable, **JVM-first** testing pyramid designed to give sub-second developer feedback without depending on heavy device emulators:

```
                     ┌───────────────────────────────┐
                     │     5% E2E & Platform         │
                     │  (Instrumented on device/AVD) │
                     │  ExoPlayer audio, WorkManager │
                     └───────────────┬───────────────┘
                                     │
                     ┌───────────────▼───────────────┐
                     │   20% Integration & UI Tests  │
                     │  (Headless on JVM/Robolectric)│
                     │ • Compose UI Behavior & Matchers│
                     │ • Roborazzi Screenshot Tests  │
                     │ • Room DAOs & FTS4 in-memory  │
                     │ • Navigation 3 Screen Flows   │
                     └───────────────┬───────────────┘
                                     │
                     ┌───────────────▼───────────────┐
                     │        75% Fast Unit Tests    │
                     │   (Pure JVM, ~10ms execution) │
                     │ • 100% ViewModels (MVI State) │
                     │ • Pure Domain UseCases        │
                     │ • Data Mappers, VTT, DTOs     │
                     │ • Shared State-Backed Fakes   │
                     └───────────────────────────────┘
```

---

## 2. Command Cheat Sheet

Always set `JAVA_HOME` to Android Studio JBR before running Gradle tasks:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
```

| Task | Command | Description |
|---|---|---|
| **Run All Unit Tests** | `./gradlew testDebugUnitTest` | Executes all local JVM tests, ViewModels, DAOs, and headless Compose tests. |
| **Run Targeted Test Class** | `./gradlew testDebugUnitTest --tests "com.example.sermonindex.ui.search.SearchViewModelTest"` | Runs a single test suite for rapid iteration. |
| **Run Targeted Test Method** | `./gradlew testDebugUnitTest --tests "*.SearchViewModelTest.searchQuery_updatesResults"` | Runs a specific test method. |
| **Record Screenshots (Roborazzi)** | `./gradlew recordRoborazziDebug` | Generates golden visual references in `src/test/snapshots/`. |
| **Verify Screenshots (Roborazzi)** | `./gradlew verifyRoborazziDebug` | Compares current rendering against golden references; fails build on visual delta. |
| **Code Coverage Report (JaCoCo)** | `./gradlew testDebugUnitTest jacocoTestReport` | Generates HTML coverage report at `app/build/reports/jacoco/jacocoTestReport/html/index.html`. |

---

## 3. Core Testing Principles

1. **JVM-First & Self-Verification**:
   - Write tests for fast execution on the JVM (`app/src/test/`).
   - Run targeted tests while developing, and run the full test suite (`./gradlew testDebugUnitTest`) before completing any task.
   - Diagnose failing tests directly from stack traces and fix the root cause — never weaken assertions or introduce arbitrary delays (`Thread.sleep`).
2. **State-Backed Fakes Over Mocks**:
   - Prefer reusable, state-backed `Fake*` implementations in `com.example.sermonindex.testing` over mocking libraries (`Mockito`, `MockK`).
   - Fakes provide realistic, deterministic behavior and evolve naturally with interface changes.
3. **MVI StateFlow Testing with Turbine**:
   - Use `runTest` and `MainDispatcherRule` for coroutine and ViewModel tests.
   - Assert ViewModel state by dispatching `FooUiEvent` and observing the resulting `FooUiState` flow using **Turbine** (`flow.test {}`).
4. **Semantics First in Compose UI**:
   - Find Composable elements using accessibility text and semantic matchers (`hasText()`, `hasContentDescription()`, `hasClickAction()`).
   - Avoid hardcoding `testTag` unless a unique semantic tree node cannot be resolved with fewer than 3 matchers.

---

## 4. Shared Test Infrastructure (`com.example.sermonindex.testing`)

Located in `app/src/test/java/com/example/sermonindex/testing/`:

| Component | Class | Purpose |
|---|---|---|
| **Coroutine Rule** | `MainDispatcherRule` | Replaces `Dispatchers.Main` with a test dispatcher (`StandardTestDispatcher` / `UnconfinedTestDispatcher`). |
| **Repository Fake** | `FakeSermonRepository` | In-memory implementation of `SermonRepository` with preloaded sermons, speakers, topics, favorites, and downloads. |
| **Playback Fake** | `FakePlaybackManager` | In-memory media controller fake exposing mutable `playbackState` and tracking play/pause/seek events. |
| **Preferences Fake** | `FakeUserPreferencesRepository` | In-memory DataStore replacement tracking reader settings, catalog updates, and dismiss banners. |
| **Model Factories** | `TestFixtures.kt` | Standard builders for domain models (`createTestSermon()`, `createTestSpeaker()`, etc.). |

---

## 5. Implementation Recipes (Code Templates)

### Recipe 1: ViewModel MVI Unit Test
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ExampleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fakeRepository = FakeSermonRepository()
    private val fakePlaybackManager = FakePlaybackManager()

    private lateinit var viewModel: ExampleViewModel

    @Before
    fun setUp() {
        fakeRepository.reset()
        fakePlaybackManager.reset()
        viewModel = ExampleViewModel(
            sermonRepository = fakeRepository,
            playbackManager = fakePlaybackManager
        )
    }

    @Test
    fun onAction_updatesUiStateDeterministically() = runTest {
        viewModel.uiState.test {
            // Initial state check
            val initial = awaitItem()
            assertFalse(initial.isLoading)

            // Dispatch UI Event
            viewModel.onEvent(ExampleUiEvent.LoadData)

            // Assert state transitions
            testDispatcher.scheduler.advanceUntilIdle()
            val loadedState = awaitItem()
            assertTrue(loadedState.items.isNotEmpty())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Recipe 2: Room In-Memory DAO Test (JVM with Robolectric)
```kotlin
@RunWith(RobolectricTestRunner::class)
class FavoritesDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var favoritesDao: FavoritesDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        favoritesDao = database.favoritesDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndObserveFavorites() = runTest {
        val entity = FavoriteSermonEntity(
            sermonId = "sermon-1",
            title = "True Repentance",
            speaker = "A.W. Tozer",
            addedAt = System.currentTimeMillis()
        )
        favoritesDao.insertFavorite(entity)

        favoritesDao.getFavorites().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("True Repentance", list[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Recipe 3: Headless Compose UI Test (Robolectric)
```kotlin
@RunWith(RobolectricTestRunner::class)
class MiniPlayerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playButton_triggersCallback() {
        var playPauseClicked = false

        composeTestRule.setContent {
            SermonIndexTheme {
                MiniPlayer(
                    mediaInfo = ActiveMediaInfo(
                        currentSermon = createTestSermon(id = "1", title = "Revival Now"),
                        playbackState = PlaybackState(isPlaying = false)
                    ),
                    progressProvider = { 0.5f },
                    onTogglePlayPause = { playPauseClicked = true },
                    onExpandClick = {},
                    onCloseClick = {}
                )
            }
        }

        // Verify title exists in semantics tree
        composeTestRule.onNodeWithText("Revival Now").assertIsDisplayed()

        // Click play button
        composeTestRule.onNodeWithContentDescription("Play").performClick()
        assertTrue(playPauseClicked)
    }
}
```

### Recipe 4: Roborazzi Visual Regression Screenshot Test
```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun miniPlayer_lightAndDarkTheme_preview() {
        captureRoboImage("mini_player_light.png") {
            SermonIndexTheme(darkTheme = false) {
                MiniPlayerPreview()
            }
        }

        captureRoboImage("mini_player_dark.png") {
            SermonIndexTheme(darkTheme = true) {
                MiniPlayerPreview()
            }
        }
    }
}
```

---

## 6. Anti-Patterns & Negative Constraints

- ❌ **DO NOT use Mockito or MockK** for domain repositories or platform managers — always use or extend state-backed Fakes in `com.example.sermonindex.testing`.
- ❌ **DO NOT use `Thread.sleep` or arbitrary coroutine delays** — use `StandardTestDispatcher` and `testDispatcher.scheduler.advanceUntilIdle()`.
- ❌ **DO NOT rely on live network or external servers** in automated tests.
- ❌ **DO NOT test generated code or Compose internals directly** (exclude them in JaCoCo).
- ❌ **DO NOT hardcode test tags** unless accessibility matchers cannot uniquely target the node.
