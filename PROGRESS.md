# Project Progress & Roadmap

> Current status, implemented features, API contracts, and roadmap for SermonIndex Android App.  
> **Current Version:** `1.6.1` | **Version Code:** `12`

---

## 1. Implemented Features & Modules

### 1.1 Audio Engine & Playback (Media3)
- **`PlaybackService`**: Background `MediaSessionService` using ExoPlayer, media notification with playback controls, lockscreen metadata, and audio attributes tuned for speech (`USAGE_MEDIA`, `CONTENT_TYPE_SPEECH`).
- **Offline File Playback**: Automatically checks local file cache and streams directly from `file://` URIs with zero network overhead when offline.
- **CDN Failover**: Automatic failover to `cdnMp3Url` when `mp3Url` fails or returns an HTTP error.
- **Sleep Timer (Таймер сна)**: Configurable timer (15m, 30m, 45m, 60m, or End of Sermon) with active countdown display and automatic audio pause upon expiration.
- **Position Tracking**: Periodic sync of playback position into `HistoryDao` every 2 seconds.
- **`PlaybackManager`**: Singleton client wrapping `MediaController`, exposing reactive `playbackState: StateFlow<PlaybackState>`.
- **`MiniPlayer`**: Persistent floating bar above bottom navigation across all primary tabs with dismiss/stop button.
- **`FullscreenPlayerScreen`**:
  - Fullscreen player with scrubber, speed selection (0.75x–2.0x with icon-only button and active color accent), seek 15s/30s, Sleep Timer dialog & countdown badge, local audio badge, download action, and empty-state handling with auto-exit.
  - **Synchronized Transcript Reading Mode (Karaoke Style)**: WebVTT cues parsing, unified 22sp typography with smooth color/weight highlight transitions, sticky playback controls header, auto-scroll anchoring keeping 3–4 preceding lines visible, instant deactivation on manual touch scroll (`NestedScrollConnection` + `DragInteraction`), and tap-to-seek functionality.

### 1.2 Screens & Presentation (Jetpack Compose + Navigation 3)
- **Navigation 3 Architecture**: 4 primary top-level tabs (Home, Bible Reader, Scripture Explorer, Library) with persistent backstacks via `rememberSaveableStateHolderNavEntryDecorator`; bottom bar strictly hidden on all sub-screens/detail routes and when drilled down into scripture chapters/verses; bottom bar reselection dispatches scroll-to-top without popping stacks or clearing state; sub-screens (Speakers, Topics) accessible via Home shelves with back navigation.
| Screen | ViewModel | Key Functionality |
|---|---|---|
| **Home** | `HomeViewModel` | Popular sermons hero carousel, "Voices of Revival" speakers shelf, "Key Themes" topics shelf, Apple Music-style chevron headers (`Title >`) navigating to full directories, "Continue Listening" history shelf, refresh support, scroll-to-top on reselect. |
| **Bible Reader** | `BibleViewModel` | Distraction-free Holy Scripture reader with 1,250+ translations, localized book titles, chapter continuous text with headings, verse markers, poetic stanzas, Hebrew subtitles, footnotes popups, table of contents sheet with OT/NT tabs and book search, reading typography settings (font size slider, Serif vs Sans toggle, line spacing), BSB audio narration streaming via Media3, in-chapter search with match navigation, and direct cross-link to Scripture sermons. |
| **Scripture Explorer** | `ScriptureViewModel` | 66 books (OT/NT), chapter picker grid, verse heat map based on sermon density, dual Scripture Verse quote card (default BSB + system language / KJV fallback), searchable translation picker bottom sheet (`ModalBottomSheet`), verse sermons list, navigation bar hidden on chapter/verse levels, synchronized back gesture handling via `BackHandler`, scroll-to-top on reselect without resetting selection. |
| **Speakers Directory** | `SpeakersViewModel` | Searchable preachers list with live in-memory filtering, shortened search placeholder, sermon count badges, and back navigation when opened from Home. |
| **Speaker Detail** | `SpeakerDetailViewModel` | Speaker portrait, biography, top topic tags, AndroidX Paging 3 paginated sermon catalog (`/v2/speakers/{slug}/page/{n}`) with `LazyPagingItems`, prefetching, loading indicator, and inline retry on failure. |
| **Topics Directory** | `TopicsViewModel` | Filterable topic grid with sermon count badges, shortened search placeholder, and back navigation when opened from Home. |
| **Topic Detail** | `TopicDetailViewModel` | Thematic description, Berean Standard Bible scripture correlations (`topScriptures`), related sermons. |
| **Sermon Detail** | `SermonDetailViewModel` | Complete sermon metadata, reactive Play/Pause state synced with `PlaybackManager` (displays `Pause` icon when active), 3-in-a-row action layout (Favorite heart, Play pill capsule, Download with progress), 3 tabs for Overview, Outline, and Study Notes. Text-only sermons automatically hide the download button and display "Read Sermon" action. |
| **Text Sermon Reader** | `TextSermonViewModel` | Dedicated distraction-free reader screen for text-only sermons (e.g., C.H. Spurgeon's written articles). Fullscreen layout mirroring player dimensions without playback controls; top app bar with favorite toggle, 220dp shadow artwork, title, speaker, "Text Sermon" badge, selectable body typography (18sp / 28sp line height), quotes, and application points. |
| **Library** | `LibraryViewModel` | 3 primary tabs with count badges (Favorites, Downloads, History), storage size indicator, "Clear Downloads" action with confirmation dialog removing local files and database records, "Clear History" action. |
| **Fullscreen Player** | `PlayerViewModel` | Comprehensive audio control, sleep timer, download action, speed selection, synchronized WebVTT karaoke transcript reading mode. |
| **Offline Banner** | N/A (Global) | Animated non-intrusive banner indicating offline mode when internet connection is lost. |

### 1.3 Downloads & Local Storage
- **`AudioDownloadWorker`**: WorkManager worker streaming MP3 files to app-specific internal storage (`audio_downloads/{id}.mp3`) with live progress reporting, network retry, and BunnyCDN fallback.
- **Downloads Deletion**: Clean architecture pipeline for single download deletion or clearing all downloads (`DeleteAllDownloadsUseCase` $\rightarrow$ `SermonRepository.deleteAllDownloads()` $\rightarrow$ `DownloadsDao.deleteAllDownloads()`), physically deleting files from disk.
- **HTTP 404 Resilience**: `Http404EmptyInterceptor` handles empty verse queries without crashing.

### 1.4 Text Sermons & Reader Mode
- **Text-Only Sermon Detection**: `isTextOnly` domain property (`!hasAudio && mp3Url.isBlank() && cdnMp3Url.isBlank()`) distinguishing text-only publications (e.g. 19th-century Spurgeon manuscripts) from audio sermons.
- **Sage Teal Visual Identity**: M3 `tertiary` tokens styled with Sage Teal (`TealPrimary = #0D9488`, `TealContainer = #CCFBF1`, `TealDark = #2DD4BF`, `TealDarkContainer = #134E4A`) applied across all text-only sermon components.
- **Adaptive `SermonListItem` Component**: Detects text-only sermons, displays book artwork placeholder (`AutoMirrored.Filled.MenuBook`), replaces duration pill with "Text" badge, styles preacher label in tertiary container color, and switches the action button from Play to Reader.
- **`TextSermonRoute` Navigation**: Registered as a dedicated fullscreen route in Navigation 3 without MiniPlayer or Offline Banner disruption, wired cleanly into all screens (Home, SpeakerDetail, TopicDetail, ScriptureExplorer, SermonDetail).

### 1.5 Architecture & Testing
- Pure domain models (`domain/model/Models.kt`), UseCases (`domain/usecase/UseCases.kt`), and pure repository interface (`SermonRepository`).
- Full Dagger Hilt DI with modules for Database, Network, Media, Dispatchers, and Repositories.
- Complete string extraction in `res/values/strings.xml` for internationalization and accessibility.
- Domain UseCase test suite (`UseCasesTest.kt`) passing via `./gradlew testDebugUnitTest`.

### 1.6 Pagination & Catalog Streaming (Paging 3)
- **`SpeakerSermonsPagingSource`**: PagingSource implementation connecting `androidx.paging:paging-runtime` and `androidx.paging:paging-compose` (version 3.3.6) with pure Kotlin domain UseCase `GetSpeakerSermonsPageUseCase`.
- **UI Integration**: `collectAsLazyPagingItems()` in `SpeakerDetailScreen` with prefetch distance, centered loading indicator during initial and append fetches, and inline error retry view on failure.
- **Unit Testing**: Complete unit test suite (`SpeakerSermonsPagingSourceTest.kt`) verifying first page, last page, and network failure mappings.

### 1.7 Holy Scripture Bible Reader Module
- **1,250+ Translations Mirror**: Integration with SermonIndex `/v2/bible/...` CDN mirror for translations across dozens of languages with locale-aware default selection (Russian Synodal `rus_syn`, Ukrainian `ukr_1996`/`ukr_pan`, English `BSB`/`eng_kjv`, Spanish `spa_r09`).
- **Structured Chapter Reader**: Continuous reading flow with section headings, verse markers with superscript numbering, poetic stanzas, Hebrew subtitles (psalms), footnotes popups, comfortable 40dp footer clearance, and next/prev chapter navigation with scroll reset to top.
- **Synchronized Auto-Hiding Toolbars**: Top bar and bottom navigation bar smoothly slide out of view when scrolling down and return when scrolling up, using identical `FastOutSlowInEasing` 250ms animation specs for seamless immersive reading.
- **Reading Typography Customization**: User preferences for font size (14sp–30sp slider), font family toggle (Serif vs Sans-Serif), and line height spacing (1.3x, 1.5x, 1.8x) with live sentence preview.
- **Table of Contents**: Modal bottom sheet with Old Testament (39) / New Testament (27) tabs, quick book search, and chapter selector grid.
- **Official Chapter Audio Narration**: Seamless integration with Media3 `PlaybackManager` streaming audio for chapters with narrator links (BSB with Gilbert, Hays, Souer); circular tonal playback button with high-contrast active state; instant audio cancellation upon chapter or language change.
- **Interactive Textual Footnotes**: Fixed API DTO parser to capture `chapter.footnotes` from SermonIndex CDN; superscript blue asterisks `*` styled with `tertiary` color without underline (`TextDecoration.None`); tap-to-inspect opens `BibleFootnoteDialog` with verse number and combined comments for verses with multiple notes, also accessible via verse tooltip button.
- **In-Chapter Search**: Real-time client-side text search highlighting matches and allowing quick navigation between occurrences.
- **Verse Tooltip & Cross-Linking**: Simple tap on any verse marker opens a tooltip with direct navigation to a dedicated `BibleVerseSermonsRoute` screen (displaying the verse in primary and secondary translations alongside related sermons) with standard back navigation.
- **Pure Isolated Navigation 3 Backstacks**: Removed artificial "exit through home" route nesting. Each top-level tab (Home, Bible, Scripture, Library) maintains its own independent backstack. Top-level screens cleanly delegate back presses to the OS for native minimization (Predictive Back on Android 12+).
- **Persistent State**: SQLite storage in `bible_reading_state` table for last read translation, book, chapter, verse, and typography settings.

---

## 2. API Contracts & Interfaces

### 2.1 Domain Repository Contract (`SermonRepository`)
```kotlin
interface SermonRepository {
    val favorites: Flow<List<FavoriteSermon>>
    val history: Flow<List<ListeningHistory>>
    val downloads: Flow<List<DownloadedSermon>>

    fun isFavorite(id: String): Flow<Boolean>
    suspend fun toggleFavorite(sermon: Sermon)

    fun isDownloaded(sermonId: String): Flow<Boolean>
    fun getDownloadProgress(sermonId: String): Flow<Int?>
    suspend fun downloadSermon(sermon: Sermon)
    suspend fun cancelDownload(sermonId: String)
    suspend fun deleteDownload(sermonId: String)
    suspend fun deleteAllDownloads()
    suspend fun getLocalAudioPath(sermonId: String): String?

    suspend fun updateHistory(sermonId: String, title: String, speaker: String, speakerImage: String, positionMs: Long, durationMs: Long)

    suspend fun getPopularSermons(): Result<List<Sermon>>
    suspend fun getScriptureBooks(): Result<List<BibleBook>>
    suspend fun getScriptureChapter(bookId: String, chapter: Int): Result<ChapterDetails>
    suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int): Result<List<Sermon>>
    suspend fun getAvailableBibleTranslations(): Result<List<BibleTranslation>>
    suspend fun getBibleVerseText(translationId: String, bookId: String, chapter: Int, verse: Int): Result<String>
    suspend fun getSpeakers(): Result<List<Speaker>>
    suspend fun getSpeakerDetails(slug: String): Result<Speaker>
    suspend fun getSpeakerSermonsPage(slug: String, page: Int): Result<SpeakerPagedSermons>
    suspend fun getTopics(): Result<List<Topic>>
    suspend fun getTopicDetails(slug: String): Result<TopicDetail>
    suspend fun getSermonDetails(id: String): Result<SermonDetail>
    suspend fun getTranscriptCues(vttUrl: String): Result<List<TranscriptCue>>

    val bibleReadingPreferences: Flow<BibleReadingPreferences>
    suspend fun getBibleChapter(translationId: String, bookId: String, chapter: Int): Result<BibleChapter>
    suspend fun getBibleBooksForTranslation(translationId: String): Result<List<BibleBook>>
    suspend fun saveBibleReadingPosition(translationId: String, bookId: String, chapter: Int, verse: Int = 1)
    suspend fun saveBibleTypographyPreferences(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float)
    suspend fun saveBibleNarrator(narrator: String?)

    suspend fun clearHistory()
}
```

### 2.2 Network Monitor Contract (`NetworkMonitor`)
```kotlin
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
```

### 2.3 Playback Manager (`PlaybackManager`)
```kotlin
class PlaybackManager(context: Context) {
    val playbackState: StateFlow<PlaybackState>
    fun playSermon(sermon: Sermon, startPositionMs: Long = 0L)
    fun togglePlayPause()
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun rewind(seconds: Int = 15)
    fun fastForward(seconds: Int = 30)
    fun setSpeed(speed: Float)
    fun setSleepTimerMinutes(minutes: Int)
    fun setSleepTimerEndOfSermon()
    fun cancelSleepTimer()
}
```

---

## 3. Next Steps (Immediate Priorities)

1. **Audio Focus & Headset Integration**:
   - Duck audio on brief notifications and handle phone calls gracefully via AudioFocusManager.
   - Wire media button receiver events for wired and Bluetooth headsets (double click forward, triple click rewind).

2. **Adaptive Layouts & Window Sizes**:
   - Two-pane player and scripture explorer layouts on foldable and tablet form factors.
   - Dynamic Material You theming based on speaker portraits.

---

## 4. Backlog & Technical Debt

- **UI Component Decomposition & Reusability (Scripture & Explorers)**: Refactor complex, multi-level screen monoliths (e.g., the 4-level navigation in `ScriptureScreen`) into decoupled, stateless UI building blocks (`ScriptureBookPicker`, `ScriptureChapterPicker`, `ScriptureVerseHeatMapGrid`, `ScriptureVerseQuoteCard`). This prevents tight coupling to single ViewModels, simplifies testing, and enables flexible composition across different sections (e.g. Bible reader, search, topic studies).
- **Room Entity Migration**: `AppDatabase.kt` currently uses `SQLiteOpenHelper` directly. Evaluate migrating to Room `@Database` once KSP Room schema export configuration is finalized.
- **Audio Focus Transition**: Smoothly duck audio on brief notifications and pause on audio focus loss (phone calls).
