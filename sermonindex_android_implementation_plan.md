# SermonIndex Android App: Comprehensive Step-by-Step Implementation Plan

## 1. Project Overview & Architecture Overview

The app is a native modern Android application for streaming and browsing over 70,000 sermons, 2,200+ preachers, and scripture-correlated preaching from the **SermonIndex API v2** (`https://api.sermonindex.net/v2`).

### 1.1 Tech Stack
* **Language & Runtime:** Kotlin (Target SDK 35+, Min SDK 26)
* **UI:** Jetpack Compose + Material 3, Single Activity architecture, Compose Navigation (or Navigation 3)
* **Architecture:** Clean Architecture + MVI/MVVM (UI -> ViewModel -> UseCases -> Repository -> DataSource)
* **Dependency Injection:** Hilt or Koin
* **Networking:** Retrofit 2 + OkHttp 4/5 + `kotlinx.serialization.json`
* **Local Storage:** Room Database (user favorites, playback history, saved positions) + DataStore Preferences
* **Audio Engine:** Jetpack Media3 (`MediaSessionService` + `ExoPlayer`)
* **Image Loading:** Coil 3 Compose
* **Background Tasks:** WorkManager (for optional offline database sync / FTS index)

```
┌──────────────────────────────────────────────────────────────────┐
│                     UI Layer (Jetpack Compose)                   │
│   HomeScreen | ScriptureExplorer | SpeakersScreen | PlayerScreen │
└─────────────────────────────────┬────────────────────────────────┘
                                  ▼
┌──────────────────────────────────────────────────────────────────┐
│               Presentation Layer (ViewModels + State)            │
└─────────────────────────────────┬────────────────────────────────┘
                                  ▼
┌──────────────────────────────────────────────────────────────────┐
│                   Domain Layer (Use Cases & Models)              │
└─────────────────────────────────┬────────────────────────────────┘
                                  ▼
┌──────────────────────────────────────────────────────────────────┐
│                        Data Layer & Engine                       │
│  ┌───────────────────────┐ ┌─────────────┐ ┌──────────────────┐  │
│  │   SermonIndexApi      │ │ Room DB     │ │ Media3 Player    │  │
│  │ (OkHttp Cache, 50MB)  │ │ (Favorites) │ │ (Failover engine)│  │
│  └───────────────────────┘ └─────────────┘ └──────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. API Quirks & Non-Negotiable Contract Rules

An agent building this app **must** follow these strict SermonIndex API constraints:
1. **No Query Parameters:** The CDN is static. Never pass `?page=`, `?limit=`, or `?q=`. All paths are fixed URL segments (e.g., `/v2/speakers/{slug}/page/{pageNumber}`).
2. **No Trailing Slashes:** Never send a trailing slash (e.g., use `/v2/topics/prayer`, NOT `/v2/topics/prayer/`). Trailing slashes return `404 Not Found`.
3. **404 HTML Error Handling:** When a scripture verse or slug has no sermons, the server returns an **HTTP 404 with an HTML document** (not JSON). The network layer must intercept 404 responses and return empty results (`emptyList()`) instead of throwing deserialization errors.
4. **Dual Audio URLs (Failover):**
   * Primary: `mp3Url` (Archive.org mirror in ~85% of cases).
   * Secondary CDN: `cdnMp3Url` (SermonIndex BunnyCDN).
   * Audio Engine must automatically fall back to `cdnMp3Url` if `mp3Url` fails, 404s, or times out.
5. **Text-Only Entries:** Sermons with `hasAudio == false` have `mp3Url == ""`. The UI and player must guard against empty audio URLs.
6. **OSIS Scripture Codes:** Scripture books use standard 3-character uppercase codes (`GEN`, `EXO`, `PSA`, `JHN`, `ROM`, `REV`).
7. **Edge Caching:** Responses contain `Cache-Control: public, max-age=2592000` (30 days). OkHttp Disk Cache must be enabled.

---

## 3. Phased Implementation Steps

### Phase 1: Foundation, Network & Data Layer

#### Step 1.1: Project Setup & Dependencies
* Initialize modern Gradle build (`build.gradle.kts` with Version Catalog `libs.versions.toml`).
* Configure dependencies:
  * `androidx.compose.*`, `androidx.compose.material3`
  * `androidx.media3:media3-exoplayer`, `media3-session`, `media3-ui`
  * `com.squareup.retrofit2:retrofit`, `okhttp3:okhttp`, `okhttp3:logging-interceptor`
  * `org.jetbrains.kotlinx:kotlinx-serialization-json`
  * `androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler`
  * `io.coil-kt.coil3:coil-compose`, `coil-network-okhttp`
  * `com.google.dagger:hilt-android` (or Koin)
* Add required permissions to `AndroidManifest.xml`:
  * `android.permission.INTERNET`
  * `android.permission.FOREGROUND_SERVICE`
  * `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  * `android.permission.POST_NOTIFICATIONS`

#### Step 1.2: DTO Models (`data/model`)
Create serializable DTOs matching the exact JSON structure:
* `SermonDto`: `id`, `slug`, `title`, `speaker`, `speakerSlug`, `speakerImage`, `summary`, `duration`, `hasAudio`, `hasTranscript`, `mp3Url`, `cdnMp3Url`, `vttUrl`, `pdfUrl`, `txtUrl`, `topics`, `scripture`.
* `SermonDetailDto`: All fields from `SermonDto` + `outline`, `quotes`, `applicationPoints`, `faq`, `transcriptHtml`, `transcript`.
* `SpeakerDto`: `slug`, `name`, `bio`, `image`, `sermonCount`, `audioCount`, `topTopics`, `totalPages`.
* `SpeakerPagedDto`: `page`, `pageSize`, `totalPages`, `total`, `next`, `prev`, `sermons: List<SermonDto>`.
* `TopicDto`: `slug`, `name`, `description`, `sermonCount`, `topScriptures`, `sermons: List<SermonDto>`.
* `ScriptureBookDto`: `bookId`, `name`, `testament`, `order`, `chapters`, `sermonCount`.
* `ScriptureChapterDto`: `bookId`, `chapter`, `sermons`, `verses: List<VerseCountDto>`.
* `VerseCountDto`: `verse`, `sermonCount`.

#### Step 1.3: OkHttp & Retrofit Configuration
* Configure `OkHttpClient`:
  * Attach `Cache(cacheDir, 50L * 1024 * 1024)` (50 MB disk cache).
  * Add custom `Http404EmptyInterceptor` or repository-level catch for HTTP 404 to convert into empty response objects instead of failing JSON parsing.
  * Timeout configuration: Connect 15s, Read 30s.
* Define `SermonIndexApiService`:
  ```kotlin
  interface SermonIndexApiService {
      @GET("sermons/popular")
      suspend fun getPopularSermons(): PopularSermonsResponse

      @GET("scripture")
      suspend fun getScriptureBooks(): ScriptureBooksResponse

      @GET("scripture/{bookId}")
      suspend fun getScriptureBook(@Path("bookId") bookId: String): ScriptureBookDto

      @GET("scripture/{bookId}/{chapter}")
      suspend fun getScriptureChapter(
          @Path("bookId") bookId: String, 
          @Path("chapter") chapter: Int
      ): ScriptureChapterDto

      @GET("scripture/{bookId}/{chapter}/{verse}")
      suspend fun getScriptureVerse(
          @Path("bookId") bookId: String, 
          @Path("chapter") chapter: Int, 
          @Path("verse") verse: Int
      ): ScriptureVerseResponse

      @GET("speakers")
      suspend fun getSpeakers(): List<SpeakerDto>

      @GET("speakers/{slug}")
      suspend fun getSpeakerDetails(@Path("slug") slug: String): SpeakerDto

      @GET("speakers/{slug}/page/{page}")
      suspend fun getSpeakerSermonsPage(
          @Path("slug") slug: String, 
          @Path("page") page: Int
      ): SpeakerPagedDto

      @GET("topics")
      suspend fun getTopics(): List<TopicDto>

      @GET("topics/{slug}")
      suspend fun getTopicDetails(@Path("slug") slug: String): TopicDto

      @GET("sermons/{id}")
      suspend fun getSermonDetails(@Path("id") id: String): SermonDetailDto
  }
  ```

#### Step 1.4: Room Database for Local State
* Define entities:
  * `FavoriteSermonEntity` (id, title, speaker, speakerImage, duration, addedAt)
  * `HistoryEntity` (sermonId, lastPositionMs, durationMs, updatedAt, completed)
  * `DownloadedSermonEntity` (sermonId, localFilePath, downloadedAt)
* Create `AppDatabase` with DAOs: `FavoritesDao`, `HistoryDao`.

---

### Phase 2: Jetpack Media3 Background Playback Engine

#### Step 2.1: Media3 Playback Service (`PlaybackService.kt`)
* Extend `MediaSessionService`.
* Instantiate `ExoPlayer` with custom `AudioAttributes` (usage: `USAGE_MEDIA`, contentType: `CONTENT_TYPE_SPEECH`).
* Implement background notification provider with lockscreen metadata, speaker portrait art, play/pause/seek controls.

#### Step 2.2: Failover Logic in Player Listener
* Create a dedicated `PlayerEventListener` monitoring `PlaybackException`:
  * If error occurs while playing `mediaItem.requestMetadata.mediaUri` (which was `mp3Url`), retrieve `cdnMp3Url` from `MediaItem.metadata` extras.
  * If `cdnMp3Url` is available and not yet tried:
    * Recreate `MediaItem` pointing to `cdnMp3Url`.
    * Retain playback position: `player.seekTo(currentPosition)`.
    * Call `player.prepare()` and `player.play()`.
* Track current playback position periodically and write to `HistoryDao`.

#### Step 2.3: Playback Controller / Repository
* Expose a reactive `PlayerState` Kotlin Flow (`currentSermon`, `isPlaying`, `currentPosition`, `duration`, `playbackSpeed`, `bufferedPosition`).
* Provide public actions: `playSermon(sermon)`, `pause()`, `resume()`, `seekTo(ms)`, `setSpeed(float)`.

---

### Phase 3: Core UI & Screens (Jetpack Compose)

#### Step 3.1: Navigation & Root Scaffold
* Setup Bottom Navigation Bar:
  1. **Home / Explore** (`/popular` + categories)
  2. **Scripture** (Bible passage sermons)
  3. **Speakers** (Preachers directory)
  4. **Topics** (Thematic catalog)
  5. **Library** (Favorites, History, Downloads)
* Add persistent **MiniPlayer** bar above BottomNavBar when a sermon is active.

#### Step 3.2: Home Screen (`HomeScreen`)
* Hero carousel: Featured/Popular sermons from `/v2/sermons/popular`.
* Quick access shelves: Top topics (Prayer, Revival, Faith, Holy Spirit).
* "Continue Listening" section sourced from `HistoryDao`.

#### Step 3.3: Scripture Explorer (`ScriptureScreen`)
* Three-level selector:
  1. **Book Selector**: 66 books grouped by Old Testament / New Testament. Display total sermons count for each book.
  2. **Chapter Grid**: Numbers 1..N.
  3. **Verse Heat Map**: Upon chapter selection, fetch `/v2/scripture/{book}/{chapter}`. Color-code verse buttons based on `verses[].sermonCount / maxCount`.
  4. **Sermons List**: When tapping a verse, show all sermons with author avatar, duration, and instant play button.

#### Step 3.4: Speakers Directory (`SpeakersScreen`)
* Fetch `/v2/speakers` (cached).
* Provide instantaneous search filter bar (filters in-memory by preacher name).
* Preacher details view (`SpeakerDetailScreen`):
  * Header with portrait (`speakerImage`), biography, and top themes (`topTopics`).
  * Infinite scrolling sermon list: paginate using `/v2/speakers/{slug}/page/{n}` until `next == null`.

#### Step 3.5: Topics Directory (`TopicsScreen`)
* Grid/List of topics with sermon count badges.
* Filter by topic name in real time.
* Topic detail view (`TopicDetailScreen`):
  * Introductory thematic text.
  * Bible reference cards (`topScriptures`) with Berean Standard Bible verse text.
  * Associated sermons list.

#### Step 3.6: Sermon Details & Fullscreen Player
* **Fullscreen Player Screen**:
  * Large speaker artwork, sermon title, preacher name.
  * Playback slider, speed selector (0.75x, 1.0x, 1.25x, 1.5x, 2.0x), 15s rewind / 30s fast-forward.
  * Tab layout:
    * **Overview**: Summary, topic tags, scripture citations.
    * **Outline**: Structured preaching points (`outline`).
    * **Transcript**: Formatted HTML reader for `transcriptHtml` or text reader for `transcript`.
    * **Study Notes**: Quotes & FAQ cards.

---

### Phase 4: Offline Search & Enhanced Sync (WorkManager)

* Add an optional setting in Settings: **"Enable Offline Full Catalog Search (~27 MB)"**.
* Create `CatalogSyncWorker` (`androidx.work.CoroutineWorker`):
  * Downloads `https://api.sermonindex.net/downloads/sermons.json.gz` (26.7 MB).
  * Streams uncompressed JSON lines into a local Room SQLite database with an `FTS4` or `FTS5` virtual table.
  * Enables instant global search query `SELECT * FROM sermon_fts WHERE sermon_fts MATCH :query` for titles, preacher names, and summaries without internet connection.

---

## 4. Verification & Testing Checklist

- [ ] **OkHttp 404 Resilience:** Verify that requesting a verse with zero sermons (e.g. 404 response) does not crash the app and returns an empty UI state with "No sermons found for this verse".
- [ ] **Media3 Failover Test:** Mock a network failure or 404 for `mp3Url`; verify player automatically seamlessly switches to `cdnMp3Url` without stopping.
- [ ] **Background Audio Playback:** Verify audio continues playing when app is minimized, screen is locked, and notification media controls function properly.
- [ ] **Audio Focus:** Ensure player pauses on incoming phone calls or when another media app starts playback.
- [ ] **Caching Verification:** Enable Airplane mode after browsing speakers or chapter; verify cached items load immediately from OkHttp disk cache.
- [ ] **OSIS Code Conformance:** Validate all 66 book codes against OSIS standard (`JHN`, `ROM`, `1CO`, etc.).
