# Project Progress & Roadmap

> Current status, implemented features, API contracts, and roadmap for SermonIndex Android App.  
> **Current Version:** `1.0.2` | **Version Code:** `3`

---

## 1. Implemented Features & Modules

### 1.1 Audio Engine & Playback (Media3)
- **`PlaybackService`**: Background `MediaSessionService` using ExoPlayer, media notification with playback controls, lockscreen metadata, and audio attributes tuned for speech (`USAGE_MEDIA`, `CONTENT_TYPE_SPEECH`).
- **CDN Failover**: Automatic failover to `cdnMp3Url` when `mp3Url` fails or returns an HTTP error.
- **Position Tracking**: Periodic sync of playback position into `HistoryDao` every 2 seconds.
- **`PlaybackManager`**: Singleton client wrapping `MediaController`, exposing reactive `playbackState: StateFlow<PlaybackState>`.
- **`MiniPlayer`**: Persistent floating bar above bottom navigation across all primary tabs with dismiss/stop button.
- **`FullscreenPlayerScreen`**: Fullscreen player with scrubber, speed selection (0.75x–2.0x), seek 15s/30s, tabs for Overview, Outline, Transcript, and Study Notes, and empty-state handling with auto-exit.

### 1.2 Screens & Presentation (Jetpack Compose + Navigation 3)
| Screen | ViewModel | Key Functionality |
|---|---|---|
| **Home** | `HomeViewModel` | Popular sermons hero carousel, top topic shelves, "Continue Listening" history shelf, refresh support. |
| **Scripture Explorer** | `ScriptureViewModel` | 66 books (OT/NT), chapter picker grid, verse heat map based on sermon density, verse sermons list. |
| **Speakers Directory** | `SpeakersViewModel` | Searchable preachers list with live in-memory filtering and sermon count badges. |
| **Speaker Detail** | `SpeakerDetailViewModel` | Speaker portrait, biography, top topic tags, infinite scrolling paginated sermon catalog (`/v2/speakers/{slug}/page/{n}`). |
| **Topics Directory** | `TopicsViewModel` | Filterable topic grid with sermon count badges. |
| **Topic Detail** | `TopicDetailViewModel` | Thematic description, Berean Standard Bible scripture correlations (`topScriptures`), related sermons. |
| **Sermon Detail** | `SermonDetailViewModel` | Complete sermon metadata, favorite toggle, play action, link to speaker profile. |
| **Library** | `LibraryViewModel` | Three tabs: Favorites, Listening History (with progress fraction & clear history), and Offline Full-Text Search. |
| **Fullscreen Player** | `PlayerViewModel` | Comprehensive audio control, speed selection, chapter outline navigation, transcript reading. |

### 1.3 Offline Capabilities & Background Sync (WorkManager)
- **`CatalogSyncWorker`**: GZIP streaming worker downloading `https://api.sermonindex.net/downloads/sermons.json.gz` (~27MB) directly into local SQLite FTS4 virtual table (`sermon_fts`) in batches.
- **Offline Search**: Instant full-text prefix and keyword search across thousands of indexed sermons without internet connection.
- **HTTP 404 Resilience**: `Http404EmptyInterceptor` handles empty verse queries without crashing.

### 1.4 Architecture & Testing
- Pure domain models (`domain/model/Models.kt`), 15 UseCases (`domain/usecase/UseCases.kt`), and pure repository interface (`SermonRepository`).
- Full Dagger Hilt DI with modules for Database, Network, Media, Dispatchers, and Repositories.
- Complete string extraction in `res/values/strings.xml` for internationalization and accessibility.
- Domain UseCase test suite (`UseCasesTest.kt`) passing via `./gradlew testDebugUnitTest`.

---

## 2. API Contracts & Interfaces

### 2.1 Domain Repository Contract (`SermonRepository`)
```kotlin
interface SermonRepository {
    val favorites: Flow<List<FavoriteSermon>>
    val history: Flow<List<ListeningHistory>>
    val ftsIndexedCount: Flow<Int>

    fun isFavorite(id: String): Flow<Boolean>
    suspend fun toggleFavorite(sermon: Sermon)
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

### 2.2 Playback Manager (`PlaybackManager`)
```kotlin
class PlaybackManager(context: Context) {
    val playbackState: StateFlow<PlaybackState>
    fun playSermon(sermon: Sermon, startPositionMs: Long = 0L)
    fun togglePlayPause()
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun seekForward(deltaMs: Long = 30_000L)
    fun seekBack(deltaMs: Long = 15_000L)
    fun setPlaybackSpeed(speed: Float)
}
```

### 2.3 Network Endpoints (`SermonIndexApiService`)
- `GET /v2/sermons/popular` -> `PopularSermonsResponse`
- `GET /v2/sermons/{id}` -> `SermonDetailDto`
- `GET /v2/scripture` -> `ScriptureBooksResponse`
- `GET /v2/scripture/{book}/{chapter}` -> `ScriptureChapterResponse`
- `GET /v2/scripture/{book}/{chapter}/{verse}` -> `ScriptureVerseResponse`
- `GET /v2/speakers` -> `SpeakersResponse`
- `GET /v2/speakers/{slug}` -> `SpeakerDetailDto`
- `GET /v2/speakers/{slug}/page/{page}` -> `SpeakerSermonsPageDto`
- `GET /v2/topics` -> `TopicsResponse`
- `GET /v2/topics/{slug}` -> `TopicDetailDto`

---

## 3. Next Steps (Immediate Priorities)

1. **Audio File Download Manager (True Offline Playback)**:
   - While catalog metadata is offline-searchable via FTS, audio itself is currently streamed from network URLs.
   - Implement `DownloadManager` / Media3 `DownloadService` to save MP3 files locally to app cache/files dir.
   - Update `PlaybackManager` to prioritize local cached audio files over remote URLs.
   - Add download progress indicator and "Downloaded" filter tab in `LibraryScreen`.

2. **Network Connectivity & Offline UX Indicators**:
   - Add a global connectivity observer (`ConnectivityManager.NetworkCallback`).
   - Display non-intrusive offline banners when disconnected, indicating cached/downloaded content availability.

3. **Paging 3 Integration**:
   - Migrate manual pagination in `SpeakerDetailViewModel` to AndroidX Paging 3 (`PagingSource` / `Pager`) for smoother infinite scrolling and prefetching.

---

## 4. Backlog & Technical Debt

- **Room Entity Migration**: `AppDatabase.kt` currently uses `SQLiteOpenHelper` directly for FTS4 speed and simplicity. Evaluate migrating to Room `@Database` with Room `@Fts4` entity once KSP Room schema export configuration is finalized.
- **UI Screenshot & Integration Tests**: Expand Compose UI tests using `createAndroidComposeRule` for navigation and playback controls.
- **Adaptive Layouts**: Integrate two-pane / list-detail layouts on tablets and foldables using Jetpack Compose adaptive navigation.
- **Audio Focus & Sleep Timer**: Add a sleep timer dialog (15m, 30m, 45m, end of sermon) in `FullscreenPlayerScreen`.
