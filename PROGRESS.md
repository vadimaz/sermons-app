# Project Progress & Roadmap

> Current status, implemented features, API contracts, and roadmap for SermonIndex Android App.  
> **Current Version:** `1.1.1` | **Version Code:** `5`

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
- **`FullscreenPlayerScreen`**: Fullscreen player with scrubber, speed selection (0.75x–2.0x), seek 15s/30s, Sleep Timer dialog & countdown badge, local audio badge, download action, dedicated inline full-text Transcript reading, and empty-state handling with auto-exit.

### 1.2 Screens & Presentation (Jetpack Compose + Navigation 3)
- **Navigation 3 Architecture**: Multi-backstack system with `rememberSaveableStateHolderNavEntryDecorator` retaining scroll positions across all tabs; bottom bar reselection dispatches scroll-to-top to active screens.
| Screen | ViewModel | Key Functionality |
|---|---|---|
| **Home** | `HomeViewModel` | Popular sermons hero carousel, top topic shelves, "Continue Listening" history shelf, refresh support, scroll-to-top on reselect. |
| **Scripture Explorer** | `ScriptureViewModel` | 66 books (OT/NT), chapter picker grid, verse heat map based on sermon density, verse sermons list, pop-to-root on reselect. |
| **Speakers Directory** | `SpeakersViewModel` | Searchable preachers list with live in-memory filtering, shortened search placeholder, and sermon count badges. |
| **Speaker Detail** | `SpeakerDetailViewModel` | Speaker portrait, biography, top topic tags, infinite scrolling paginated sermon catalog (`/v2/speakers/{slug}/page/{n}`). |
| **Topics Directory** | `TopicsViewModel` | Filterable topic grid with sermon count badges and shortened search placeholder. |
| **Topic Detail** | `TopicDetailViewModel` | Thematic description, Berean Standard Bible scripture correlations (`topScriptures`), related sermons. |
| **Sermon Detail** | `SermonDetailViewModel` | Complete sermon metadata, 3-in-a-row action layout (Favorite heart, Play pill capsule, Download with progress), 3 tabs for Overview, Outline, and Study Notes. |
| **Library** | `LibraryViewModel` | Four single-word tabs with Material 3 `BadgedBox` count badges (Favorites, Downloads, History, Search), storage badge & delete action, instant offline search. |
| **Fullscreen Player** | `PlayerViewModel` | Comprehensive audio control, sleep timer, download action, speed selection, and dedicated inline transcript reading. |
| **Offline Banner** | N/A (Global) | Animated non-intrusive banner indicating offline mode when internet connection is lost. |

### 1.3 Offline Capabilities & Background Sync (WorkManager)
- **`AudioDownloadWorker`**: WorkManager worker streaming MP3 files to app-specific internal storage (`audio_downloads/{id}.mp3`) with live progress reporting, network retry, and BunnyCDN fallback.
- **`CatalogSyncWorker`**: GZIP streaming worker downloading `https://api.sermonindex.net/downloads/sermons.json.gz` (~27MB) directly into local SQLite FTS4 virtual table (`sermon_fts`) in batches.
- **Offline Search**: Instant full-text prefix and keyword search across thousands of indexed sermons without internet connection.
- **HTTP 404 Resilience**: `Http404EmptyInterceptor` handles empty verse queries without crashing.

### 1.4 Architecture & Testing
- Pure domain models (`domain/model/Models.kt`), 20 UseCases (`domain/usecase/UseCases.kt`), and pure repository interface (`SermonRepository`).
- Full Dagger Hilt DI with modules for Database, Network, Media, Dispatchers, and Repositories.
- SQLite `AppDatabase` v2 schema migration supporting `favorites`, `history`, `downloads`, and `sermon_fts`.
- Complete string extraction in `res/values/strings.xml` for internationalization and accessibility.
- Domain UseCase test suite (`UseCasesTest.kt`) passing via `./gradlew testDebugUnitTest`.

---

## 2. API Contracts & Interfaces

### 2.1 Domain Repository Contract (`SermonRepository`)
```kotlin
interface SermonRepository {
    val favorites: Flow<List<FavoriteSermon>>
    val history: Flow<List<ListeningHistory>>
    val downloads: Flow<List<DownloadedSermon>>
    val ftsIndexedCount: Flow<Int>

    fun isFavorite(id: String): Flow<Boolean>
    suspend fun toggleFavorite(sermon: Sermon)

    fun isDownloaded(sermonId: String): Flow<Boolean>
    fun getDownloadProgress(sermonId: String): Flow<Int?>
    suspend fun downloadSermon(sermon: Sermon)
    suspend fun cancelDownload(sermonId: String)
    suspend fun deleteDownload(sermonId: String)
    suspend fun getLocalAudioPath(sermonId: String): String?

    suspend fun updateHistory(sermonId: String, title: String, speaker: String, speakerImage: String, positionMs: Long, durationMs: Long)

    suspend fun getPopularSermons(): Result<List<Sermon>>
    suspend fun getScriptureBooks(): Result<List<BibleBook>>
    suspend fun getScriptureChapter(bookId: String, chapter: Int): Result<ChapterDetails>
    suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int): Result<List<Sermon>>
    suspend fun getSpeakers(): Result<List<Speaker>>
    suspend fun getSpeakerDetails(slug: String): Result<Speaker>
    suspend fun getSpeakerSermonsPage(slug: String, page: Int): Result<SpeakerPagedSermons>
    suspend fun getTopics(): Result<List<Topic>>
    suspend fun getTopicDetails(slug: String): Result<TopicDetail>
    suspend fun getSermonDetails(id: String): Result<SermonDetail>

    suspend fun searchOffline(query: String): List<OfflineSearchResult>
    suspend fun clearOfflineIndex()
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

1. **Paging 3 Integration**:
   - Migrate manual pagination in `SpeakerDetailViewModel` to AndroidX Paging 3 (`PagingSource` / `Pager`) for smoother infinite scrolling, prefetching, and state preservation.

2. **Adaptive Layouts (Foldables & Tablets)**:
   - Integrate two-pane / list-detail layouts on wide screens and foldables using Jetpack Compose adaptive navigation.

3. **UI Screenshot & Integration Tests**:
   - Expand Compose UI tests using `createAndroidComposeRule` for navigation, playback controls, and offline UI verification.

---

## 4. Backlog & Technical Debt

- **Room Entity Migration**: `AppDatabase.kt` currently uses `SQLiteOpenHelper` directly for FTS4 speed and simplicity. Evaluate migrating to Room `@Database` with Room `@Fts4` entity once KSP Room schema export configuration is finalized.
- **Audio Focus Transition**: Smoothly duck audio on brief notifications and pause on audio focus loss (phone calls).
