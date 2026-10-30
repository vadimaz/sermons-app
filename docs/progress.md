# Project Progress & Roadmap

> Current status, implemented features, API contracts, and roadmap for SermonIndex Android App.  
> **Current Version:** `1.15.1` | **Version Code:** `36`

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
  - **Recomposition Optimization (`TranscriptCueItem`)**: WebVTT cues extracted into an isolated skippable `@Composable TranscriptCueItem` with memoized seek callbacks and primitive active flags (`isActive`, `isMatch`), skipping 100% of inactive transcript items during audio playback ticks, reducing slow draw commands by 90% and eliminating Vsync misses on 120Hz displays.

### 1.2 Screens & Presentation (Jetpack Compose + Navigation 3)
- **Navigation 3 Architecture & Motion Transitions**: 5 primary top-level tabs (Home, Search, Bible Reader, Scripture Explorer, Library) with persistent backstacks via `rememberSaveableStateHolderNavEntryDecorator` and `rememberViewModelStoreNavEntryDecorator`, following the canonical "exit through Home" fixed start destination pattern; Material 3 **Fade Through** (`fadeOut` 90ms + `fadeIn` 210ms + `scaleIn` 0.94f -> 1.0f) transitions between all 5 top-level tabs (including "exit through Home" return) and Material 3 **Shared X-Axis** (`18%` horizontal slide + fade) transitions for hierarchical drill-down and back navigation (`transitionSpec`, `popTransitionSpec`, `predictivePopTransitionSpec`); bottom bar strictly hidden on all sub-screens/detail routes and when drilled down into scripture chapters/verses; bottom bar reselection dispatches scroll-to-top without popping stacks or clearing state; sub-screens (Speakers, Topics) accessible via Home shelves with back navigation.
| Screen | ViewModel | Key Functionality |
|---|---|---|
| **Home** | `HomeViewModel` | Popular sermons hero carousel, "Voices of Revival" speakers shelf, "Key Themes" topics shelf, Apple Music-style chevron headers (`Title >`) navigating to full directories, "Continue Listening" history shelf, refresh support, scroll-to-top on reselect. |
| **Search** | `SearchViewModel` | Global search across speakers, topics, and 70k+ offline sermon catalog with debounced FTS4 queries; dynamic category filter chips (All, Sermons, Speakers, Topics); dismissible catalog sync status banner; recent search history; sermon cards with Play pill, Favorite toggle, quote snippet with query highlights, and differentiated navigation (card body -> Sermon Details, book icon -> Text Reader). |
| **Bible Reader** | `BibleViewModel` | Distraction-free Holy Scripture reader with 1,250+ translations, localized book titles, chapter continuous text with headings, verse markers, poetic stanzas, Hebrew subtitles, footnotes popups, table of contents sheet with OT/NT tabs and book search, reading typography settings, BSB audio narration streaming via Media3, in-chapter search, single-verse selection mode via long-press, pastel highlights (Yellow, Green, Blue, Coral) with auto-save and removal, ModalBottomSheet anchored to bottom bar, 1:1 quote card sharing generator, clipboard integration, and scroll position preservation across navigation. |
| **Scripture Explorer** | `ScriptureViewModel` / `ScriptureChapterViewModel` | Multi-route Navigation 3 flow (`ScriptureRoute` $\rightarrow$ `ScriptureChaptersRoute` $\rightarrow$ `ScriptureVerseHeatMapRoute` $\rightarrow$ shared `BibleVerseSermonsRoute`) with Material 3 Shared X-Axis transitions and native Predictive Back: 66 books (OT/NT), chapter picker grid, verse heat map based on sermon density with `@AssistedInject` `ScriptureChapterViewModel`, unified dual Scripture Verse quote card and verse sermons list via `BibleVerseSermonsScreen`, and scroll-to-top on reselect. |
| **Speakers Directory** | `SpeakersViewModel` | Searchable preachers list with live in-memory filtering, shortened search placeholder, sermon count badges, and back navigation when opened from Home. |
| **Speaker Detail** | `SpeakerDetailViewModel` | Speaker portrait, biography, top topic tags, AndroidX Paging 3 paginated sermon catalog (`/v2/speakers/{slug}/page/{n}`) with `LazyPagingItems`, prefetching, loading indicator, and inline retry on failure. |
| **Topics Directory** | `TopicsViewModel` | Filterable topic grid with sermon count badges, shortened search placeholder, and back navigation when opened from Home. |
| **Topic Detail** | `TopicDetailViewModel` | Thematic description, Berean Standard Bible scripture correlations (`topScriptures`), related sermons. |
| **Sermon Detail** | `SermonDetailViewModel` | Complete sermon metadata, reactive Play/Pause state synced with `PlaybackManager` (displays `Pause` icon when active), 3-in-a-row action layout (Favorite heart, Play pill capsule, Download with progress), 3 tabs for Overview, Outline, and Study Notes. Text-only sermons automatically hide the download button and display "Read Sermon" action. Tapping Play on a newly selected sermon automatically navigates to Fullscreen Player, while tapping Play/Pause on an already active sermon (playing or paused) toggles playback in-place. |
| **Text Sermon Reader** | `TextSermonViewModel` | Dedicated distraction-free reader screen for text-only sermons (e.g., C.H. Spurgeon's written articles). Fullscreen layout mirroring player dimensions without playback controls; top app bar with favorite toggle, 220dp shadow artwork, title, speaker, "Text Sermon" badge, selectable body typography (18sp / 28sp line height), quotes, and application points. |
| **Media Library Hub** | `LibraryViewModel` | Apple Music-inspired media library hub featuring Material 3 flexible `MediumTopAppBar` with collapse on scroll and zero redundant insets; vertical navigation menu with Favorites, Downloads, Scripture Bookmarks (History menu row removed); lower section converted into "History" grid displaying recent sermons viewed, read, listened to, or watched, with instant "Clear History" button; gear icon top action opening Settings Center. |
| **Favorites** | `LibraryViewModel` | Dedicated sub-screen (`FavoritesRoute`) displaying favorite sermons list with immediate playback, removal, and detail navigation. App bar and Scaffold configured with zero top inset duplicate. |
| **Downloads** | `LibraryViewModel` | Dedicated sub-screen (`DownloadsRoute`) displaying offline downloaded sermons, storage usage badge, individual deletion, and clear all confirmation dialog. App bar and Scaffold configured with zero top inset duplicate. |
| **History** | `LibraryViewModel` | Dedicated sub-screen (`HistoryRoute`) displaying playback history with progress bars, resume playback at exact timestamp, and clear history action. App bar and Scaffold configured with zero top inset duplicate. |
| **Scripture Bookmarks** | `LibraryViewModel` | Dedicated sub-screen (`BibleBookmarksRoute`) with color-coded highlights, verse quotes, 1:1 quote card sharing generator, and 1-tap jump to Bible reader. App bar and Scaffold configured with zero top inset duplicate. |
| **Settings Center** | `SettingsViewModel` | Comprehensive settings screen (`SettingsRoute`): Appearance section with System Default Theme switch (title and description) which disables manual Light/Dark chips when enabled; Dynamic Colors removed; Storage & Cache section with live cache calculation and 1-tap clear, with redundant "Downloaded Audio" row removed to avoid duplication with Library; Offline Catalog section with 3-state adaptive action button (Download Primary Button / Update Filled Tonal Button / Delete Destructive Outlined Button with confirmation dialog); auto-update check disabled by default (`isAutoCatalogUpdateCheckEnabled = false`) with runtime notification permission request on toggle; app version `1.15.1 (36)` and SermonIndex.net attribution. |
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
- **Composable Architecture & Multipreview Support (`@ThemePreviews`)**:
  - Established two-tier component package structure: Core Design System in `ui/components/` and feature-scoped subcomponents in `ui/<feature>/components/` (`ui/home/components/`, `ui/library/components/`, `ui/sermon/components/`, `ui/player/components/`, `ui/speakers/components/`, `ui/topics/components/`).
  - Decomposed bloated monolithic screen files: `HomeScreen.kt` (880 $\rightarrow$ 264 lines), `LibraryScreen.kt` (755 $\rightarrow$ 232 lines), `SermonDetailScreen.kt` (659 $\rightarrow$ 390 lines), and `CommonComponents.kt` into isolated Single Responsibility files (`LoadingView.kt`, `ErrorView.kt`, `EmptyStateView.kt`, `SearchTextField.kt`).
  - Created centralized `@ThemePreviews` annotation in `theme/ThemePreviews.kt` providing dual Light & Dark Material 3 preview rendering.
  - Introduced `PreviewFixtures.kt` supplying shared mock data across all component previews without test or production clutter.
- Shared JVM unit testing infrastructure in `com.example.sermonindex.testing` (`MainDispatcherRule`, `FakeSermonRepository`, `FakeUserPreferencesRepository`, `FakePlaybackManager`) with `kotlinx-coroutines-test` and `CashApp Turbine`, governed by [`AGENTS.md`](../AGENTS.md) and [`docs/testing.md`](testing.md).
- **Agent Guidelines & Planning Conventions (`AGENTS.md`)**: Formalized Section 5 establishing strict English language requirements for implementation plans, task lists, and architectural artifacts, paired with Russian chat summaries and excerpts.

### 1.6 Pagination & Catalog Streaming (Paging 3)
- **`SpeakerSermonsPagingSource`**: PagingSource implementation connecting `androidx.paging:paging-runtime` and `androidx.paging:paging-compose` (version 3.5.3) with pure Kotlin domain UseCase `GetSpeakerSermonsPageUseCase`.
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
- **Navigation 3 Multi-Backstack ("Exit Through Home")**: Each top-level tab (Home, Search, Bible, Scripture, Library) maintains its own independent backstack with state and ViewModel retention. Pressing Back from the root of any secondary tab returns to `HomeRoute` (`startRoute`) before delegating back presses to the OS for native minimization (Predictive Back on Android 12+).
- **Persistent State**: SQLite storage in `bible_reading_state` table for last read translation, book, chapter, verse, and typography settings.

### 1.8 Search & Offline Catalog (SQLite FTS4 + REST API Fallback)
- **Top-Level Navigation**: 5th primary destination added to bottom navigation bar (`SearchRoute`), positioned between Home and Bible Reader with persistent backstack support.
- **Unified Search Interface (`SearchScreen`)**:
  - Live search input with leading search icon, clear button, and recent search history with single/all query deletion.
  - Multi-category filtering chips: **All** (Все), **Sermons** (Проповеди), **Preachers** (Проповедники), and **Topics** (Темы) displayed dynamically when query is present.
  - Categorized results rendering: Speakers shelf/cards with avatar & sermon count, Topics chips with count badges, and Sermon items with duration, speaker, direct play/favorite interaction, and quote snippets.
  - Differentiated card interactions: tapping the sermon card body navigates to `SermonDetailScreen` (overview, outline, scripture, notes), while tapping the book icon button navigates directly to `TextSermonScreen` reader.
  - Keyboard action search triggering with IME hide.
- **Zero-Download First Experience & Online Fallback**:
  - Out of the box, searches across popular sermons, all preachers, and all topics downloaded via official REST API v2 endpoints with in-memory caching.
  - Non-intrusive catalog download recommendation banner ("Unlock 70,000+ sermons offline" / 26.7 MB gzip) allowing voluntary download, with dismiss action persisted in `SearchPreferences`.
- **High-Performance SQLite FTS4 Virtual Table (`sermon_fts`) & Self-Healing Architecture**:
  - Native virtual table `sermon_fts USING fts4(sermon_id, title, speaker, summary, duration, speaker_image, has_audio, mp3_url, cdn_mp3_url, tokenize=unicode61)` in `AppDatabase.kt`.
  - Sub-millisecond full-text queries matching prefix words (`term*`) across title, speaker, and summary.
  - Multilingual `tokenize=unicode61` enabling case-insensitive search across Cyrillic and international alphabets.
  - Adaptive column extraction via `SELECT *` preventing schema mismatch crashes across table revisions.
  - Self-healing database lifecycle in `DatabaseHelper.onOpen`: clears orphaned `catalog_metadata` when `sermon_fts` is empty, accurately returning `CatalogSyncState.NotDownloaded`.
- **Streaming Catalog Ingestion (`CatalogSyncWorker`)**:
  - WorkManager `CoroutineWorker` streaming `https://api.sermonindex.net/downloads/sermons.json.gz` using `GZIPInputStream` and streaming JSON parser.
  - Batched SQLite transactions (1,000 records/batch) preventing UI thread lock and memory pressure.
  - Live progress reporting observed by `SearchViewModel`.
  - Automatic `recreateSermonFtsTable()` at the start of syncing to ensure clean table initialization.
- **Catalog Versioning & Update Detection**:
  - `GET /v2/stats` integration fetching server dataset timestamp (`generated`).
  - Compared with local `catalog_metadata` table; displays an update available badge/banner if a newer dataset is detected on the CDN.
- **Async Room SQLite Pre-Warming**: Asynchronous background initialization of the Room SQLite connection pool and schema PRAGMAs in `SermonApp.onCreate()` via `@IoDispatcher`, speeding up cold application startup by 21% (down to 437 ms) and increasing SQLite page cache hits to 172.

### 1.9 User Preferences & State Persistence (Jetpack DataStore)
- **Preferences DataStore Architecture**: Replaced legacy `SharedPreferences` and single-row SQLite table `bible_reading_state` with modern, reactive `androidx.datastore:datastore-preferences:1.3.0`.
- **Clean Architecture Boundaries**: Extracted pure domain contract `UserPreferencesRepository` and UseCases (`GetFirstTranslationUseCase`, `SetFirstTranslationUseCase`, `GetSecondTranslationUseCase`, `SetSecondTranslationUseCase`). Removed direct `data.local` dependencies from `ScriptureViewModel` and `BibleVerseSermonsViewModel`.
- **Pure Direct DataStore**: No legacy migration overhead needed during pre-release development — preferences flow directly into and out of DataStore with clean defaults.
- **Removed SQLite Tech Debt**: Dropped `bibleChangeSignal` flow polling, `bible_reading_state` SQLite table, and `BibleReadingStateEntity` completely.

### 1.10 Android Room Database & Local Storage Architecture
- **Room v2.8.5 Migration**: Completely replaced 670 lines of low-level `SQLiteOpenHelper`, raw SQL queries, and manual `ContentValues` with canonical Modern Android Development (MAD) Room `@Database` (version 1) and KSP2 code generation.
- **Full-Text Search FTS4 Virtual Table**: Integrated `@Entity(tableName = "sermon_fts")` with `@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)` with explicit `rowid` handling, preserving sub-millisecond search performance across 70,000+ sermon titles, speakers, and transcripts.
- **Automatic Reactive Invalidation Tracker**: Eliminated 5 manual `MutableStateFlow` change signals (`favoritesChangeSignal`, `historyChangeSignal`, `downloadsChangeSignal`, `catalogChangeSignal`, `searchHistoryChangeSignal`). Room's SQLite `InvalidationTracker` automatically emits fresh data to `Flow<T>` on any table mutation from UI or background workers.
- **Clean Architecture Repository Decoupling**: Refactored `SermonRepositoryImpl` to inject pure DAO interfaces (`FavoritesDao`, `HistoryDao`, `DownloadsDao`, `SearchDao`) instead of depending directly on `AppDatabase`.
- **KSP Schema Export**: Automated Room JSON schema export to `app/schemas/` for rigorous compile-time verification and future migration testing.

### 1.11 Video Playback Engine, Picture-in-Picture & Fullscreen Immersive Mode
- **Dual Audio/Video Engine (Media3 & ExoPlayer)**:
  - Direct video rendering via `VideoPlayerSurface` and `PlayerView` (Media3 UI) hooked into `PlaybackManager.player`.
  - Offline `.mp4` video downloads supported via WorkManager (`AudioDownloadWorker` with `KEY_IS_VIDEO` and `.mp4` storage) and streamed seamlessly offline from local storage.
  - `DownloadedSermon.toSermon()` maps `.mp4` downloads to `hasVideo = true` and `mp4Url = "file://$localPath"`.
- **Viewport Visibility-Driven Audio/Video Decoding**:
  - Automatically monitors whether the player header video component is in the viewport (`firstVisibleItemIndex == 0`).
  - When scrolled off-screen (e.g., to read synced WebVTT transcript) or when the player is minimized to the mini-player, video decoding is immediately suspended to conserve battery, CPU, and memory, continuing seamless audio-only decoding.
  - Resumes video rendering instantaneously when scrolled back into view or when navigating to fullscreen video.
- **Picture-in-Picture (PiP) Architecture**:
  - Android 12+ `setAutoEnterEnabled(isPlaying)` allows fluid gestures directly from fullscreen video to a 16:9 floating PiP window on swipe-to-home.
  - `LocalPictureInPictureMode` provides reactive PiP state down Compose trees, auto-hiding all controls in the floating window.
  - Closing the PiP window via the system 'X' button invokes `MainActivity.onStop()` to immediately halt playback and clear the foreground media notification.
- **True Immersive Fullscreen Mode (`FullscreenVideoScreen`)**:
  - Unlocked `SCREEN_ORIENTATION_SENSOR` for natural portrait or landscape video viewing without forced rotations.
  - System bars (status and navigation) hidden via `WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` and `hide(systemBars())`.
  - Zero `Scaffold` insets (`contentWindowInsets = WindowInsets(0, 0, 0, 0)` and `PaddingValues(0.dp)`) so the video physically extends to 100% of the display.
  - Fully transparent system bars in theme and window, contrast enforcement disabled (`isStatusBarContrastEnforced = false`, `isNavigationBarContrastEnforced = false`), and `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` for edge-to-edge presentation without letterboxing.
  - `.safeDrawingPadding()` protects fullscreen overlay controls from display cutouts, notches, and rounded corners.
- **Player & Card Refinements**:
  - Restored speaker portraits as primary imagery across all sermon cards, hero carousel, mini player, and details screen.
  - Numerical speed indicator badge (`1x`, `1.25x`, etc.) replacing the gauge icon.
  - Removed redundant "Offline Audio" chip from fullscreen player header for clean presentation.
  - Strictly excluded Bible chapter tracks (`bible_...`) from history queries and continue-listening shelves.

### 1.12 Periodic Catalog Update Worker & Push Notifications (WorkManager)
- **Automated Update Check Worker (`CatalogUpdateCheckWorker`)**:
  - Power-efficient WorkManager `CoroutineWorker` scheduled periodically (every 24 hours with a 6-hour flex interval) on `NetworkType.CONNECTED` and `requiresBatteryNotLow(true)`.
  - Compares remote `/v2/stats` `generated` timestamp with local `catalog_metadata.generatedAt` without downloading heavy datasets.
  - Automatically respects user preference `isAutoCatalogUpdateCheckEnabled` and records `lastCatalogUpdateCheckTimestamp`.
- **System Notification Delivery**:
  - Dedicated notification channel `catalog_updates_channel` with importance `DEFAULT`.
  - Android 13+ (API 33+) safe permission checks ensuring zero crashes or exceptions if notifications are disabled.
  - Deep-link intent targeting `MainActivity` with `EXTRA_NAVIGATE_TO_SEARCH = true` to immediately navigate to the Search screen where `CatalogSyncBanner` displays update status.
  - One-tap notification action button ("Update Now" / `R.string.catalog_update_notification_action_update`) dispatching a broadcast to `CatalogUpdateBroadcastReceiver`.
- **`CatalogUpdateBroadcastReceiver`**:
  - Unexported broadcast receiver listening for `ACTION_START_CATALOG_UPDATE`.
  - Cancels the system notification, immediately schedules `CatalogSyncWorker` (`ExistingWorkPolicy.REPLACE`), and shows a confirmation toast.
- **Runtime Notification Permissions**:
  - Non-intrusive runtime `POST_NOTIFICATIONS` permission request via Compose `rememberLauncherForActivityResult` in `MainActivity` for Android 13+ devices.
- **Clean Architecture & Preference Use Cases**:
  - `UserPreferencesRepository` contracts and single-responsibility domain UseCases (`GetAutoCatalogUpdateCheckEnabledUseCase`, `SetAutoCatalogUpdateCheckEnabledUseCase`, `GetLastCatalogUpdateCheckTimestampUseCase`, `SetLastCatalogUpdateCheckTimestampUseCase`).

### 1.13 Comprehensive SermonIndex API v2 Specification
- **Single Source of Truth ([`docs/api.md`](api.md))**: Complete English technical reference document describing the entire SermonIndex API v2 platform, Edge CDN distribution, zero-authentication model, rate-limiting policies, and HTTP conventions.
- **Complete Endpoints & Contracts**: Fully documented request/response schemas for Metadata (`/v2`, `/v2/stats`), Scripture Index (`/v2/scripture/...` with chapter verse heatmaps), Topics (`/v2/topics/...` with BSB texts), Speakers & 50-item Pagination (`/v2/speakers/...`), Sermons & 500-item Pagination (`/v2/sermons/...`, `/v2/sermons/{id}` with 39 fields), Random discovery shards (`/v2/random/{1..80}`), and hosted Bible corpus (1,253 translations and 6 historical commentaries).
- **Offline & Media Resilience**: Formalized media failover patterns (Internet Archive primary with automatic BunnyCDN fallback), bulk database specifications (`sermonindex.db` SQLite 3 and `sermons.json.gz`), Kiwix offline ZIM packages, and OSIS canonical book codes.
- **Android Integration Guidelines**: Clear technical conventions for Retrofit interceptors, Room FTS4 sync, Media3 ExoPlayer error recovery, and Paging 3 pagination sources.

### 1.14 Random Sermons Discovery Shards ("Surprise Me" / Vintage Treasures)
- **80 Static Pre-computed Shards (`/v2/random/{1..80}`)**: Full integration with SermonIndex static random shards containing 50 curated sermons each (4,000 total rotating sermons) with minified keys (`i`, `t`, `s`, `g`, `m`, `d`, `a`, `c`, `y`, `p`, `v`, `x`) delivering a featherweight ~9 KB JSON payload per shard.
- **Clean Architecture Pipeline**:
  - `RandomShardResponse` and `RandomSermonDto` with `@SerialName` annotations.
  - `RandomSermonDto.toDomain(): Sermon` extension mapping in `Mappers.kt`.
  - Pure domain use case `GetRandomSermonsUseCase` invoking `SermonRepository.getRandomSermons(shard: Int?)`.
  - Concrete implementation in `SermonRepositoryImpl` with `@IoDispatcher` context switching and fallback to random shard selection (`1..80`).
- **Presentation & UI (`HomeScreen`)**:
  - "Vintage Treasures" shelf positioned on `HomeScreen` between Topics and Most Listened Sermons with eyebrow header ("DISCOVERY") and one-tap Shuffle action.
  - Interactive Shuffle button with live `CircularProgressIndicator` during fetches, refreshing sermons from another random shard while preserving scroll and other shelves.
  - Horizontally scrolling cards with speaker artwork, 2-line title, preacher name, duration/text badge, and instant Play/Read action.
- **Automated Test Coverage**: Comprehensive deserialization and mapping test suite (`RandomSermonMappingTest`) and usecase test (`UseCasesTest`).

### 1.15 Bible Bookmarks, Pastel Highlights & 1:1 Quote Cards Generator
- **Verse Selection Mode**:
  - Long press on any Bible verse activates focused selection mode for that specific verse, displaying subtle selection background tint.
  - Tapping another verse while in selection mode cleanly shifts selection to the tapped verse.
- **Material 3 Modal Bottom Sheet (`BibleVerseBottomSheet`)**:
  - Anchored modal sheet with scrim preventing accidental background taps on reader text.
  - Extended bottom container color (`surfaceContainerHigh`) with `navigationBarsPadding()` eliminating transparent bar gaps above the system navigation bar.
  - Pastel color palette row featuring 4 highlights: Yellow (`#FEF08A`), Green (`#BBF7D0`), Blue (`#BFDBFE`), Coral (`#FECDD3`).
  - Auto-saving on color selection: tapping any palette color highlights the verse, persists the bookmark to Room and DataStore, dismisses the sheet, and shows a "Saved" toast (`R.string.toast_saved`).
  - Contextual "Remove" action (`TextButton` styled with `colorScheme.error`): appears only when the verse already has a highlight, deletes the bookmark, resets highlighting, dismisses the sheet, and shows a "Removed" toast (`R.string.toast_removed`).
  - Native clipboard copy with verse text, book name, chapter, verse, and translation reference; gated to Android 12 and below (`Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2`) to prevent duplicate toast notifications alongside Android 13+ native clipboard overlay.
- **1:1 Quote Card Generator & Sharing (`QuoteCardSheet`)**:
  - High-resolution (1080x1080) square image quote cards with 4 selectable pastel backgrounds matching highlight tones.
  - Offloaded native Android `Canvas` + `StaticLayout` bitmap generation running asynchronously on `@IoDispatcher` (`generateQuoteCardBitmap`).
  - Horizontal scrollable background color chips with embedded color preview circles.
  - Android system Share Sheet integration via `FileProvider` (`file_paths.xml`), creating cached PNG cards without persistent storage bloat.
- **Library Bookmarks Tab**:
  - 4th tab in Library with count badge, displaying all user bookmarks sorted by timestamp.
  - Bookmarks cards with pastel color indicators, reference, full verse text preview, and quick share / delete actions.
  - One-tap navigation to Bible reader with automatic smooth scrolling to the exact bookmarked verse.
- **Room Migration & Database Integrity**:
  - Official Room migration `MIGRATION_1_2` creating table `bible_bookmarks` with indices on `(translation_id, book_id, chapter)`.
  - Exported schema verification (`2.json`).
- **Reader State & Navigation Fixes**:
  - Preserved reader scroll position across navigation tabs via `rememberSaveable` chapter key tracking.
  - DataStore reading position timestamp tracking (`positionUpdatedAt: Long = 0L` in `BibleReadingPreferences`) ensuring reliable auto-scroll on bookmark navigation without false-positive triggers during typography or narrator adjustments.

### 1.16 Dependency Stack Modernization & Android 37 Alignment
- **Gradle & Tooling Upgrades**: Upgraded Gradle wrapper to `9.8.2` and AGP to `9.4.1`, updating `compileSdk` to `37` (Android 37.0).
- **Kotlin & Compiler Ecosystem**: Updated to Kotlin `2.3.21`, KSP `2.3.12`, Coroutines `1.11.0`, and Kotlinx Serialization `1.11.0`.
- **Jetpack Libraries**: Updated Compose BOM to `2026.09.00` (Material 3 `1.4.0`), Navigation 3 to `1.2.0`, Lifecycle to `2.11.0`, WorkManager to `2.12.0`, Paging 3 to `3.5.3`, and DataStore to `1.3.0`.
- **Media & Networking**: Updated AndroidX Media3 to `1.11.1`, Retrofit to `3.0.0`, OkHttp to `5.5.1`, and Coil 3 to `3.6.3`.
- **Unit Test File System Isolation**: Hardened `UserPreferencesRepositoryTest` to isolate DataStore file handles using per-test `backgroundScope` and single-mutation test execution.

### 1.17 Modern API Alignment, Zero-Warning Build & Search Virtualization
- **Navigation 3 & Hilt Compose Lifecycle**:
  - Wired `rememberViewModelStoreNavEntryDecorator<NavKey>()` alongside `rememberSaveableStateHolderNavEntryDecorator<NavKey>()` in `Navigation.kt` to properly scope and clear `ViewModelStore` instances per navigation entry.
  - Migrated all 14 screens from deprecated `androidx.hilt.navigation.compose.hiltViewModel` to `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`.
- **Media3 Native Compose UI (`media3-ui-compose`)**:
  - Replaced legacy `AndroidView(PlayerView)` wrappers in `VideoPlayerSurface.kt` and `FullscreenVideoScreen.kt` with official `ContentFrame` from `androidx.media3:media3-ui-compose:1.11.1`.
- **Modern Platform APIs & Zero Compiler Warnings**:
  - Enabled `-Xannotation-default-target=param-property` for Kotlin 2.3+.
  - Migrated `BibleScreen.kt` from deprecated `LocalClipboardManager` to `LocalClipboard` and `ClipEntry`.
  - Removed deprecated window color/cutout flags (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`, `statusBarColor`, `navigationBarColor`) in favor of `enableEdgeToEdge()`, and stripped redundant `SDK_INT` checks below `minSdk = 26`.
- **Networking, WorkManager & Room `@Upsert` Optimization**:
  - Deduplicated `@Singleton OkHttpClient` across `NetworkModule` and `NetworkClient`; migrated `AudioDownloadWorker` from `HttpURLConnection` to the shared `OkHttpClient`.
  - Injected `@IoDispatcher` across `AudioDownloadWorker`, `CatalogSyncWorker`, and `CatalogUpdateCheckWorker`.
  - Migrated Room DAOs (`FavoritesDao`, `DownloadsDao`, `SearchDao`, `BibleBookmarksDao`) from `@Insert(onConflict = REPLACE)` to SQLite-native `@Upsert`, and added `ClearHistoryUseCase`.
- **Topics & Speakers Search Performance (14,087 Topics Catalog)**:
  - Replaced non-lazy `FlowRow` in `SearchScreen` (`SearchCategory.TOPICS`) with lazy `LazyColumn` virtualization (`TopicSearchItem`) and `contentType` keys across all lazy lists.
  - Offloaded in-memory filtering of 14,087 topics (`TopicsViewModel`) and 2,210 preachers (`SpeakersViewModel`) from the Main UI thread to `@IoDispatcher` using `mapLatest`.
  - Replaced `.lowercase().contains(q)` with zero-allocation `contains(cleanQuery, ignoreCase = true)` in `SermonRepositoryImpl.search()`, skipped unnecessary FTS count queries when filtering by `TOPICS`/`SPEAKERS`, and prevented `LazyColumn` composition teardown during active typing or category chip switches.
- **Scripture Explorer Navigation 3 Refactoring & Deduplication**:
  - Replaced 4-level internal state switching in `ScriptureScreen` with canonical Navigation 3 routes (`ScriptureRoute` $\rightarrow$ `ScriptureChaptersRoute` $\rightarrow$ `ScriptureVerseHeatMapRoute` $\rightarrow$ `BibleVerseSermonsRoute`), enabling Material 3 Shared X-Axis transitions and native Predictive Back gestures across all levels.
  - Extracted `@HiltViewModel(assistedFactory = ...)` `ScriptureChapterViewModel` for the verse heat map level and unified Level 4 with `BibleVerseSermonsScreen` / `BibleVerseSermonsViewModel`, eliminating ~250 lines of duplicate translation and sermon loading logic and removing the `isScriptureAtRoot` workaround from `Navigation.kt`.

### 1.18 Jetpack Compose UI & Recomposition Performance Optimization
- **Root State Decoupling (`ActiveMediaInfo`)**:
  - Separated fast-moving audio playback position polling (500ms ExoPlayer ticks) from structural playback metadata via `@Immutable data class ActiveMediaInfo`.
  - Migrated `MainNavigation` to observe `activeMediaInfo: StateFlow<ActiveMediaInfo>` instead of `playbackState`.
  - Completely eliminated 120 unneeded recompositions per minute across root `Scaffold`, 5-item `NavigationBar`, and Navigation 3 entry decorators during media playback.
- **Draw-Phase Progress Isolation**:
  - Refactored `MiniPlayer` to accept `ActiveMediaInfo` and a lambda provider `progressProvider: () -> Float`, evaluating progress in the draw phase of `LinearProgressIndicator` without triggering full component recompositions.
- **Transcript Karaoke Acceleration ($O(1)$ and $O(\log N)$)**:
  - Replaced linear cue scan in `PlayerViewModel` with binary search `binarySearchBy { it.startMs }` ($O(\log N)$).
  - Hoisted active cue evaluation before `itemsIndexed` in `FullscreenPlayerScreen`.
  - Converted transcript search match check from `List<Int>` to `Set<Int>` ($O(1)$ lookup).
  - Added `contentType = { _, _ -> "transcript_cue" }` to allow slot table recycling.
- **Lazy Layout Virtualization & Slot Recycling**:
  - Added explicit stable keys and `contentType` attributes across all lazy lists and grids (`HomeScreen`, `LibraryScreen`, `ScriptureScreen`, `SpeakerDetailScreen`, `TopicDetailScreen`).
  - Replaced range list allocation `(1..totalChapters).toList()` in `ScriptureChaptersScreen` with zero-allocation `items(count = totalChapters, key = { it + 1 }, contentType = { "chapter_cell" })`.
- **Bible Reader In-Item Pre-Indexing & Caching**:
  - Pre-indexed chapter bookmarks and footnotes into hash maps once at the chapter level in `BibleReaderContent` (`associateBy`, `groupBy`) instead of running linear searches inside each verse row during scrolling.
  - Wrapped `buildAnnotatedString` in `remember` keyed by verse content and theme colors.
  - Assigned stable semantic keys (`"verse_${item.number}"`) to chapter items.
- **Static Regex Hoisting & Layout Flattening**:
  - Precompiled and hoisted static regexes (`HTML_TAG_REGEX`, `WHITESPACE_REGEX`, `TOKEN_DELIMITER_REGEX`) to file top-level scopes across `TextHighlightUtils`, `TopicDetailScreen`, and `FullscreenPlayerScreen`.
  - Flattened triple-nested layout nodes (`Box -> Surface -> Box -> Icon`) in `SermonListItem` to a single `Box` with background and alignment.

### 1.19 Modern JVM Testing Architecture & Test Pyramid (MAD 2026)
- **Universal JVM-First Strategy**:
  - Completely replaced fragile instrumented stub tests with 100% JVM-executable tests running in ~14 seconds via `./gradlew testDebugUnitTest`.
  - Established canonical testing standard governed by [`AGENTS.md`](../AGENTS.md) and [`docs/testing.md`](testing.md).
- **In-Memory Room Database & Migration Testing**:
  - In-memory SQLite testing using `Room.inMemoryDatabaseBuilder()` and Robolectric: `FavoritesDaoTest`, `HistoryDaoTest`, `DownloadsDaoTest`, `BibleBookmarksDaoTest`, and `SearchDaoAndFtsTest` (verifying FTS4 prefix matching and multilingual unicode61 tokenization).
  - Production database migration verification (`DatabaseMigrationTest` verifying `MIGRATION_1_2` schema transitions via `MigrationTestHelper`).
- **MVI / MVVM StateFlow Testing**:
  - 100% coverage of core ViewModels (`HomeViewModel`, `LibraryViewModel`, `PlayerViewModel`, `SpeakersViewModel`, `TopicsViewModel`, `ScriptureViewModel`, `SearchViewModel`, `BibleViewModel`, `TextSermonViewModel`).
  - Verifying initial states, asynchronous effect handling, and state transitions using `kotlinx-coroutines-test` (`runTest`), `StandardTestDispatcher`, and `CashApp Turbine` (`flow.test {}`).
  - Pure state-backed fakes in `com.example.sermonindex.testing` (`FakeSermonRepository`, `FakeUserPreferencesRepository`, `FakePlaybackManager`) ensuring predictable, deterministic behavior without mock runtime overhead.
- **Headless Jetpack Compose UI Testing (Robolectric + `createComposeRule`)**:
  - JVM-based semantic node assertions and user interaction dispatch without needing emulators or physical devices.
  - Tested core components and screens: `CommonViewsTest` (`LoadingView`, `ErrorView`, `EmptyStateView`), `MiniPlayerTest` (playback controls, progress fractions, expand gestures), `SermonListItemTest` (audio and text badges, play/read actions, highlight queries), `LibraryScreenTest` (tab bar navigation, empty states), and `SearchScreenTest` (search input, query update dispatch, dynamic category filter chips).
- **Code Coverage & Reporting (JaCoCo)**:
  - Automated `./gradlew jacocoTestReport` task integrated into Gradle KTS.
  - HTML and XML report generation with surgical exclusion rules filtering out compiler-generated metadata, Dagger/Hilt components, Room DAO implementations, and Android framework classes.
- **Total Test Suite**: 145 tests passing cleanly across JVM in under 15 seconds.

### 1.20 Media Library Hub (Apple Music Style) & Comprehensive Settings Center (v1.15.0)
- **Apple Music-Inspired Media Library Hub (`LibraryScreen.kt`)**:
  - Replaced horizontal tabs with a vertical navigation menu featuring accent-tinted icons (Favorites: Rose, Downloads: Sage Teal, History: Amber, Bookmarks: Indigo), live count badges, and chevron links.
  - Added "Recently Added" (`recently_added`) section featuring a 2-column adaptive card grid combining listening history and downloads with square artwork, tap-to-play buttons, playback progress bars, and detail navigation.
  - Added Settings action icon in the top app bar for instant access to the Settings Center.
- **Dedicated Sub-Screens with Navigation 3 Transitions**:
  - Created standalone destinations with `TopAppBar` and shared X-axis transitions: `FavoritesScreen` (`FavoritesRoute`), `DownloadsScreen` (`DownloadsRoute`), `HistoryScreen` (`HistoryRoute`), `BibleBookmarksScreen` (`BibleBookmarksRoute`), and `SettingsScreen` (`SettingsRoute`).
  - Automatic bottom navigation bar hiding on all sub-screens with seamless Predictive Back support.
- **Comprehensive Settings Center (`SettingsScreen.kt` & `SettingsViewModel.kt`)**:
  - **Appearance Section**: User-selectable theme mode (System Default, Light, Dark) and dynamic Material You Monet palette extraction on Android 12+ (API 31+).
  - **Storage & Cache Section**: Live temporary cache size calculation via `@Singleton CacheManager` with 1-tap clear, plus offline downloads storage stats with direct navigation.
  - **Catalog Section**: FTS4 offline catalog indexing status, auto-update check toggle, and manual catalog sync trigger.
  - **About Section**: App version `1.15.0 (35)`, SermonIndex.net attribution, and open-source ministry statement.
- **Global Reactive Theming (`MainActivity.kt` & `Theme.kt`)**:
  - Connected `themeMode` and `useDynamicColor` preferences directly from `UserPreferencesRepository` into `SermonIndexTheme`, switching themes instantaneously across the entire Compose tree without app restarts.
- **Expanded Test Suite**: Added `SettingsViewModelTest`, updated `LibraryViewModelTest`, `LibraryScreenTest`, and `UserPreferencesRepositoryTest`, expanding the test suite to 157 passing tests.

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
    suspend fun getRandomSermons(shard: Int? = null): Result<List<Sermon>>
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

    val bibleBookmarks: Flow<List<BibleBookmark>>
    fun getBibleBookmarksForChapter(translationId: String, bookId: String, chapter: Int): Flow<List<BibleBookmark>>
    suspend fun saveBibleBookmark(translationId: String, bookId: String, bookName: String, chapter: Int, verse: Int, text: String, color: HighlightColor)
    suspend fun deleteBibleBookmark(translationId: String, bookId: String, chapter: Int, verse: Int)
    suspend fun deleteBibleBookmarkById(id: Long)

    val catalogSyncState: Flow<CatalogSyncState>
    fun search(query: String, category: SearchCategory): Flow<SearchAggregateResult>
    suspend fun downloadOrUpdateCatalog()
    suspend fun cancelCatalogDownload()
    suspend fun checkForCatalogUpdate(): Result<Boolean>
    suspend fun deleteCatalog()
    val recentSearchQueries: Flow<List<String>>
    suspend fun addRecentSearchQuery(query: String)
    suspend fun clearRecentSearchQueries()

    suspend fun clearHistory()
    suspend fun recordSermonView(sermonId: String, title: String, speaker: String, speakerImage: String)
}
```

### 2.2 Network Monitor Contract (`NetworkMonitor`)
```kotlin
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
```

### 2.3 User Preferences Contract (`UserPreferencesRepository`)
```kotlin
interface UserPreferencesRepository {
    val firstTranslationId: Flow<String>
    val secondTranslationId: Flow<String>
    suspend fun setFirstTranslation(id: String)
    suspend fun setSecondTranslation(id: String)
    val isCatalogReadyBannerDismissed: Flow<Boolean>
    suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean)
    val bibleReadingPreferences: Flow<BibleReadingPreferences>
    suspend fun saveBibleReadingPosition(translationId: String, bookId: String, chapter: Int, verse: Int)
    suspend fun saveBibleTypography(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float)
    suspend fun saveBibleNarrator(narrator: String?)
    val isAutoCatalogUpdateCheckEnabled: Flow<Boolean>
    suspend fun setAutoCatalogUpdateCheckEnabled(enabled: Boolean)
    val lastCatalogUpdateCheckTimestamp: Flow<Long>
    suspend fun setLastCatalogUpdateCheckTimestamp(timestamp: Long)
    val themeMode: Flow<AppThemeMode>
    suspend fun setThemeMode(mode: AppThemeMode)
}
```

### 2.4 Playback Manager (`PlaybackManager`)
```kotlin
open class PlaybackManager(context: Context? = null) {
    open val playbackState: StateFlow<PlaybackState>
    open val activeMediaInfo: StateFlow<ActiveMediaInfo>
    open val progressFraction: Float
    open val player: Player?
    fun setVideoActive(active: Boolean)
    open fun playSermon(sermon: Sermon, startPositionMs: Long = 0L)
    open fun togglePlayPause()
    fun pause()
    fun resume()
    fun stop()
    open fun seekTo(positionMs: Long)
    fun rewind(seconds: Int = 15)
    fun fastForward(seconds: Int = 30)
    open fun setSpeed(speed: Float)
    open fun setSleepTimerMinutes(minutes: Int)
    open fun setSleepTimerEndOfSermon()
    open fun cancelSleepTimer()
}

@Immutable
data class ActiveMediaInfo(
    val currentSermon: Sermon? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isUsingFailoverCdn: Boolean = false,
    val isLocalAudio: Boolean = false
)
```

### 2.5 Room Local Data Access Contracts (DAOs)
```kotlin
@Dao
interface FavoritesDao {
    fun getAllFavorites(): Flow<List<FavoriteSermonEntity>>
    fun isFavorite(id: String): Flow<Boolean>
    suspend fun insertFavorite(entity: FavoriteSermonEntity)
    suspend fun deleteFavorite(id: String): Int
}

@Dao
interface HistoryDao {
    fun getRecentHistory(limit: Int = 30): Flow<List<HistoryEntity>>
    suspend fun getHistory(sermonId: String): HistoryEntity?
    suspend fun upsertHistory(entity: HistoryEntity)
    suspend fun deleteHistory(sermonId: String): Int
    suspend fun clearHistory(): Int
}

@Dao
interface DownloadsDao {
    fun getAllDownloads(): Flow<List<DownloadedSermonEntity>>
    suspend fun getDownload(id: String): DownloadedSermonEntity?
    fun isDownloaded(id: String): Flow<Boolean>
    suspend fun insertDownload(entity: DownloadedSermonEntity)
    suspend fun deleteDownload(id: String): Int
    suspend fun deleteAllDownloads(): Int
    suspend fun getTotalStorageBytes(): Long
}

@Dao
interface SearchDao {
    suspend fun searchSermonsFts(query: String, limit: Int): List<SermonFtsEntity>
    suspend fun countSermonsFts(query: String): Int
    suspend fun getFtsSermonCount(): Int
    suspend fun insertSermonsFtsBatch(records: List<SermonFtsEntity>)
    suspend fun clearSermonsFts(): Int
    fun getCatalogMetadataFlow(): Flow<CatalogMetadataEntity?>
    suspend fun getCatalogMetadata(): CatalogMetadataEntity?
    suspend fun saveCatalogMetadata(metadata: CatalogMetadataEntity)
    suspend fun clearCatalogMetadata(): Int
    fun getRecentSearchQueries(limit: Int = 10): Flow<List<String>>
    suspend fun insertSearchQuery(query: SearchHistoryEntity)
    suspend fun deleteSearchQuery(query: String): Int
    suspend fun clearSearchHistory(): Int
}

@Dao
interface BibleBookmarksDao {
    fun getBookmarksForChapter(translationId: String, bookId: String, chapter: Int): Flow<List<BibleBookmarkEntity>>
    fun getAllBookmarks(): Flow<List<BibleBookmarkEntity>>
    suspend fun getBookmarkForVerse(translationId: String, bookId: String, chapter: Int, verse: Int): BibleBookmarkEntity?
    suspend fun upsertBookmark(bookmark: BibleBookmarkEntity)
    suspend fun deleteBookmarkForVerse(translationId: String, bookId: String, chapter: Int, verse: Int)
    suspend fun deleteBookmark(id: String)
}
```

---

## 3. Next Steps (Immediate Priorities)

1. **Audio Focus & Headset Integration**:
   - Duck audio on brief notifications and handle phone calls gracefully via AudioFocusManager.
   - Wire media button receiver events for wired and Bluetooth headsets (double click forward, triple click rewind).

---

## 4. Backlog & Technical Debt

- **Bible Verse Sharing Enhancements**: Add deep links into the app and optional SermonIndex branding badge / Google Play link to generated quote cards (deferred per user review).
- **User Bible Notes & Journaling**: Add personal reflections and notes attached to highlighted verses or chapters (deferred per user review).
- **Adaptive Layouts & Navigation 3 Scenes (Foldables, Tablets, Desktops)**: Multi-pane responsive layout architecture using Navigation 3 Scenes (`ListDetailSceneStrategy` for Speakers and Topics directories with placeholder detail panes, `SupportingPaneSceneStrategy` for two-pane player with synchronized WebVTT transcript, adaptive start `NavigationRail` replacing bottom bar on medium/expanded windows, and `GridCells.Adaptive` across directories, library, and search). Detailed technical plan preserved in `adaptive_design_plan.md`. Deferred for future release.
- **Dynamic Material You Theming**: Palette extraction based on speaker portraits / album art.
- **Bible Full-Text Search (SQLite FTS)**: Pure offline indexing of active Bible translation verses into an SQLite FTS virtual table (`bible_fts`) for sub-millisecond Bible search across Old and New Testaments.
