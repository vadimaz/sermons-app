# SermonIndex API v2 — Specification & Architecture Guide for AI Agents

> **Single Source of Truth** for AI assistants, engineers, and architecture planners designing and implementing network, persistence, sync, and media playback layers for the SermonIndex application.

---

## 1. Executive Summary & Core Architectural Model

The **SermonIndex API (v2)** is a high-performance, open-access, edge-distributed REST-like CDN API. It provides programmatic access to the world's largest digital archive of historical Christian preaching: **70,400+ sermons**, **2,210+ preachers**, **14,080+ topical categories**, and **342,500+ scripture citations**, alongside a CDN mirror of the **Free Use Bible API** (1,253 global Bible translations and 6 historical commentaries).

### Architectural Characteristics:

| Property | Value | Architectural Context |
|---|---|---|
| **Base URL** | `https://api.sermonindex.net/v2` | No trailing slash on endpoints |
| **Delivery Mechanism** | BunnyCDN Global Edge Servers | Pre-generated static JSON files distributed globally |
| **Authentication** | **None** | No API keys, Bearer tokens, headers, or registration required |
| **Rate Limiting** | **None** | Unlimited requests, but clients must cache and respect bandwidth |
| **HTTP Methods** | `GET`, `HEAD` | Read-only. Any mutating verb (`POST`, `PUT`, `DELETE`) returns `405` |
| **CORS** | `Access-Control-Allow-Origin: *` | Safe for direct browser-to-CDN calls without proxying |
| **Content-Type** | `application/json; charset=utf-8` | Uniform JSON format across all API endpoints |
| **Edge Cache** | `Cache-Control: public, max-age=2592000` | 30-day edge cache, invalidated on catalog rebuilds |
| **File Extension** | Optional `.json` | `/v2/stats` and `/v2/stats.json` resolve identically |
| **Licensing** | Ministry & Personal Use | Free for churches, research, and non-profit apps. Commercial sale prohibited. Attribution appreciated |

---

## 2. Protocol Conventions, Constraints & Error Handling

1. **Path-Based Parameters Only (No Query Strings)**:
   - BunnyCDN edge servers serve pre-rendered static files and **do not parse query strings**.
   - Queries like `?page=2` or `?limit=50` are completely ignored.
   - All pagination, IDs, and filters must be path segments (e.g., `/v2/sermons/page/2`, `/v2/speakers/{slug}/page/{n}`).
2. **Strict Prohibition of Trailing Slashes**:
   - A URL with a trailing slash (e.g., `/v2/topics/prayer/`) returns `404 Not Found`.
   - Correct form: `/v2/topics/prayer`.
3. **HTTP 404 Returns HTML (Never JSON)**:
   - When a requested resource does not exist (such as an un-preached scripture verse or an invalid slug), the server returns **HTTP 404 with an HTML document** (not a JSON error object).
   - **Android Client Rule**: The networking layer must verify `response.isSuccessful` prior to parsing JSON to prevent `JsonDecodingException`. In this codebase, [`Http404EmptyInterceptor`](file:///c:/Users/vzore/AntigravityProjects/Sermons%20App/app/src/main/java/com/example/sermonindex/data/network/Http404EmptyInterceptor.kt) intercepts 404 responses for scripture lookups and safely substitutes an empty JSON payload.
4. **Guaranteed Non-Null Field Values**:
   - Empty values are consistently serialized as `""` (empty string) or `[]` (empty list) rather than `null` or omitted keys, allowing safe property navigation without defensive null checks.
5. **Strict Case Sensitivity**:
   - Bible book codes must be **uppercase OSIS codes** (`GEN`, `JHN`, `ROM`). A lowercase request (`/v2/scripture/jhn`) yields `404`.
   - Speaker and topic slugs are **lowercase hyphenated** (`leonard-ravenhill`, `holy-spirit`).
6. **Immutable Sermon Identifiers**:
   - Sermon IDs are opaque 16-character alphanumeric strings (e.g., `GX7CRS1lybv-Zow8`). These IDs are permanent and immutable across dataset rebuilds.

---

## 3. Media Streaming & CDN Failover Strategy

SermonIndex distributes audio and video through a hybrid infrastructure designed to balance load between the Internet Archive and dedicated high-speed CDNs:

- **`mp3Url` / `mp4Url`**: Points to the Internet Archive mirror whenever available (~85% of sermons). Falls back to the SermonIndex CDN if not mirrored.
- **`cdnMp3Url` / `cdnMp4Url`**: Direct SermonIndex CDN copy (`https://sermonindex1.b-cdn.net/...`).
- **`archiveAudioUrl` / `archiveVideoUrl`**: Explicit Internet Archive mirror URL.
- **`vttUrl`**: WebVTT synchronized subtitle track on `sermonindex3.b-cdn.net` (used for Karaoke-style transcript display).
- **`srtUrl`**: SubRip subtitle track.
- **`pdfUrl` / `txtUrl`**: Downloadable complete transcript documents.

> [!WARNING]
> Browsers and standard media players (including ExoPlayer) **do not automatically failover** between multiple `<source>` tags upon HTTP 404 or network timeout.
> The media engine ([`PlaybackService`](file:///c:/Users/vzore/AntigravityProjects/Sermons%20App/app/src/main/java/com/example/sermonindex/playback/PlaybackService.kt)) must explicitly listen for media playback errors on `mp3Url` and seamlessly re-prepare playback using `cdnMp3Url`.

---

## 4. Endpoints Reference & Data Contracts

### 4.1 Service Descriptor & Catalog Metadata

#### `GET /v2` (or `/v2.json`)
Root service description containing API version, live stats, rebuild timestamp, license notes, and endpoint map.
```json
{
  "name": "SermonIndex API",
  "version": "2",
  "baseUrl": "https://api.sermonindex.net/v2",
  "documentation": "https://www.sermonindex.net/api/",
  "downloads": "https://www.sermonindex.net/api/downloads/",
  "license": "Free to use. Attribution to SermonIndex.net appreciated.",
  "stats": {
    "sermons": 70419,
    "speakers": 2210,
    "topics": 14087,
    "scriptureBooks": 66,
    "verseReferences": 342558,
    "generated": "2026-09-21T02:29:31Z"
  },
  "endpoints": { ... }
}
```

#### `GET /v2/stats` (or `/v2/stats.json`)
Ultra-lightweight endpoint returning live item counts and the `generated` ISO-8601 rebuild timestamp.
> [!TIP]
> Use this endpoint for periodic background workers (`CatalogUpdateCheckWorker`) to check for dataset updates without wasting bandwidth.
```json
{
  "sermons": 70419,
  "speakers": 2210,
  "topics": 14087,
  "scriptureBooks": 66,
  "verseReferences": 342558,
  "generated": "2026-09-21T02:29:31Z"
}
```

---

### 4.2 Scripture → Sermons Index

#### `GET /v2/scripture`
Catalog of all 66 canonical books with testament (`OT`/`NT`), canonical order, total chapter counts, and aggregate sermon references.
```json
{
  "count": 66,
  "totalReferences": 342558,
  "books": [
    {
      "bookId": "GEN",
      "name": "Genesis",
      "testament": "OT",
      "order": 1,
      "chapters": 50,
      "sermonCount": 8830,
      "apiUrl": "https://api.sermonindex.net/v2/scripture/GEN"
    }
  ]
}
```

#### `GET /v2/scripture/{bookId}` (e.g. `/v2/scripture/JHN`)
Detailed book metadata listing every chapter containing sermons, along with each chapter's sermon count.
```json
{
  "bookId": "JHN",
  "name": "John",
  "testament": "NT",
  "order": 43,
  "chapters": 21,
  "sermonCount": 31784,
  "chaptersWithSermons": [
    {
      "chapter": 1,
      "sermonCount": 3021,
      "apiUrl": "https://api.sermonindex.net/v2/scripture/JHN/1"
    },
    {
      "chapter": 3,
      "sermonCount": 4625,
      "apiUrl": "https://api.sermonindex.net/v2/scripture/JHN/3"
    }
  ]
}
```

#### `GET /v2/scripture/{bookId}/{chapter}` (e.g. `/v2/scripture/JHN/3`)
Every sermon preached on that chapter, plus a per-verse count breakdown (`verses` array) to construct a passage **preaching heat map**.
```json
{
  "bookId": "JHN",
  "book": "John",
  "chapter": 3,
  "reference": "John 3",
  "sermonCount": 4312,
  "verses": [
    { "verse": 1, "sermonCount": 79, "apiUrl": "https://api.sermonindex.net/v2/scripture/JHN/3/1" },
    { "verse": 16, "sermonCount": 2504, "apiUrl": "https://api.sermonindex.net/v2/scripture/JHN/3/16" }
  ],
  "sermons": [ /* Array of Sermon Card objects */ ]
}
```

#### `GET /v2/scripture/{bookId}/{chapter}/{verse}` (e.g. `/v2/scripture/JHN/3/16`)
All sermons citing that exact verse.
```json
{
  "bookId": "JHN",
  "book": "John",
  "chapter": 3,
  "verse": 16,
  "reference": "John 3:16",
  "sermonCount": 2504,
  "sermons": [ /* Array of Sermon Card objects */ ]
}
```
*Note: Returns 404 HTML if no sermon cites this specific verse.*

---

### 4.3 Topics & Thematic Index

#### `GET /v2/topics`
All 14,087 theological topics, ordered descending by sermon count. Each item includes a description and top scripture correlations.
```json
{
  "count": 14087,
  "topics": [
    {
      "slug": "holy-spirit",
      "name": "Holy Spirit",
      "description": "The Holy Spirit, often referred to as the 'Holy Ghost'...",
      "url": "https://www.sermonindex.net/topics/holy-spirit/",
      "apiUrl": "https://api.sermonindex.net/v2/topics/holy-spirit",
      "sermonCount": 3068,
      "topScriptures": [
        {
          "text": "Acts 1:8",
          "bookId": "ACT",
          "chapter": 1,
          "verse": 8,
          "count": 54,
          "bsb": "But you will receive power when the Holy Spirit comes upon you..."
        }
      ]
    }
  ]
}
```

#### `GET /v2/topics/{slug}` (e.g. `/v2/topics/prayer`, `/v2/topics/revival`)
Complete topic entry: theological introduction, top scriptures with Berean Standard Bible (BSB) text, and every sermon filed under this theme.
```json
{
  "slug": "prayer",
  "name": "Prayer",
  "description": "In the Bible, prayer is predominantly understood as...",
  "url": "https://www.sermonindex.net/topics/prayer/",
  "apiUrl": "https://api.sermonindex.net/v2/topics/prayer",
  "sermonCount": 1410,
  "topScriptures": [ ... ],
  "sermons": [ /* Array of Sermon Card objects */ ]
}
```

---

### 4.4 Preachers & Contributors

#### `GET /v2/speakers` (or `/v2/contributors`)
Complete directory of all 2,210 preachers.
*Note: `/v2/speakers` serializes the list under the `"speakers"` key, while `/v2/contributors` uses `"contributors"`.*
```json
{
  "count": 2210,
  "speakers": [
    {
      "slug": "ch-spurgeon",
      "name": "C.H. Spurgeon",
      "bio": "Charles Haddon Spurgeon (1834-1892) was an English Particular Baptist...",
      "image": "https://www.sermonindex.net/images/speakers/c/chspurgeon.png",
      "url": "https://www.sermonindex.net/speakers/ch-spurgeon/",
      "apiUrl": "https://api.sermonindex.net/v2/speakers/ch-spurgeon",
      "sermonCount": 4129,
      "audioCount": 178,
      "imageCount": 63,
      "topTopics": [
        { "name": "Spiritual Growth", "slug": "spiritual-growth", "count": 30 }
      ]
    }
  ]
}
```

#### `GET /v2/speakers/{slug}` (e.g. `/v2/speakers/leonard-ravenhill`)
Preacher profile containing biography, portrait, top topics, and **all** sermons.
> [!CAUTION]
> For prolific speakers, this payload is huge (Zac Poonen's file is 8.5 MB for 3,362 sermons). For incremental UI lists, always use the paged endpoint.

#### `GET /v2/speakers/{slug}/page/{n}` (e.g. `/v2/speakers/zac-poonen/page/1`)
Paginated sermons for a preacher, exactly 50 sermons per page.
- Available only for preachers with > 50 sermons.
- Contains absolute URL links for `next` and `prev` navigation (`next` is `null` on the last page).
```json
{
  "page": 1,
  "pageSize": 50,
  "totalPages": 68,
  "total": 3362,
  "next": "https://api.sermonindex.net/v2/speakers/zac-poonen/page/2",
  "prev": null,
  "sermons": [ /* 50 Sermon Card objects */ ]
}
```

---

### 4.5 Sermons Catalog & Detailed Records

#### `GET /v2/sermons/popular`
The 500 most-listened audio sermons in SermonIndex history. Ideal for the "Featured" hero shelf on home screens.
```json
{
  "count": 500,
  "sermons": [ /* 500 Sermon Card objects */ ]
}
```

#### `GET /v2/sermons/page/{n}` (Pages 1 to 141)
The entire catalog of 70,419 sermons delivered in pages of 500 items. Start at page 1.
```json
{
  "page": 1,
  "pageSize": 500,
  "totalPages": 141,
  "total": 70419,
  "next": "https://api.sermonindex.net/v2/sermons/page/2",
  "prev": null,
  "sermons": [ /* 500 Sermon Card objects */ ]
}
```

#### `GET /v2/sermons`
Massive single JSON file containing all 70,419 sermons (~80 MB+). Use bulk downloads instead of calling this directly.

#### `GET /v2/sermons/{id}` (Complete Sermon Record)
The definitive deep record for an individual sermon:

```json
{
  "id": "GX7CRS1lybv-Zow8",
  "slug": "the-love-of-god",
  "title": "The Love of God",
  "url": "https://www.sermonindex.net/speakers/leonard-ravenhill/the-love-of-god/",
  "apiUrl": "https://api.sermonindex.net/v2/sermons/GX7CRS1lybv-Zow8",
  "speaker": "Leonard Ravenhill",
  "speakerSlug": "leonard-ravenhill",
  "speakerUrl": "https://www.sermonindex.net/speakers/leonard-ravenhill/",
  "speakerImage": "https://www.sermonindex.net/images/speakers/l/leonardravenhill.png",
  "speakerBio": "Leonard Ravenhill (1907-1994) was a British revival preacher...",
  "summary": "The sermon explores the transformative power of God's love...",
  "description": "Full multi-sentence description...",
  "sermonType": "topical",
  "mediaType": "AUDIO",
  "hasAudio": true,
  "hasTranscript": true,
  "duration": "46:12",
  "videoUrl": "",
  "mp3Url": "https://archive.org/download/SERMONINDEX_SID0478/SID0478.mp3",
  "mp4Url": "",
  "cdnMp3Url": "https://sermonindex1.b-cdn.net/0/SID0478.mp3",
  "cdnMp4Url": "",
  "archiveAudioUrl": "https://archive.org/download/SERMONINDEX_SID0478/SID0478.mp3",
  "archiveVideoUrl": "",
  "srtUrl": "https://sermonindex3.b-cdn.net/srt/0/SID0478.srt",
  "vttUrl": "https://sermonindex3.b-cdn.net/vtt/0/SID0478.vtt",
  "pdfUrl": "https://www.sermonindex.net/pdf-text/pdf/l/leonard-ravenhill--the-love-of-god.pdf",
  "txtUrl": "https://www.sermonindex.net/pdf-text/txt/l/leonard-ravenhill--the-love-of-god.txt",
  "thumbnailUrl": "https://www.sermonindex.net/images/speakers/l/leonardravenhill.png",
  "image": "https://www.sermonindex.net/images/speakers/l/leonardravenhill.png",
  "topics": [
    { "name": "Love", "slug": "love" }
  ],
  "scripture": [
    { "text": "John 3:16", "bookId": "JHN", "book": "John", "chapter": 3, "verse": 16 }
  ],
  "keywords": ["God's love", "revival", "cross", "salvation"],
  "outline": [
    {
      "heading": "I",
      "points": ["The manifestation of love", "The cost of grace"]
    }
  ],
  "faq": [
    { "q": "What is the central message?", "a": "God's sacrificial love for humanity." }
  ],
  "quotes": [
    "We do not know God. We know about him."
  ],
  "applicationPoints": [
    "Live each day with the awareness of God's presence."
  ],
  "transcriptHtml": "<p>And today I'd like to speak...</p>",
  "transcript": "And today I'd like to speak..."
}
```

---

### 4.6 Random Shards & Embed Ecosystem

For widgets and "Random Sermon" discovery, the API provides 80 static pre-generated shards:
- **`GET /v2/random/{1..80}`** (or `/v2/random/{1..80}.json`):
  Each shard contains 50 curated sermons (4,000 total rotating sermons) with minified keys for minimal bandwidth consumption (~9 KB payload):
  - `i`: `id`
  - `t`: `title`
  - `u`: `url`
  - `s`: `speaker`
  - `g`: `speakerSlug`
  - `m`: `speakerImage`
  - `d`: `duration`
  - `a`: `mp3Url` (Archive.org)
  - `c`: `cdnMp3Url` (BunnyCDN)
  - `y`: `summary`
  - `p`: `[topicName, topicSlug]`
  - `v`: `[scriptureText, scripturePath]`
  - `x`: `hasTranscript` (1 if true)

- **Embed Script**: `https://www.sermonindex.net/embed/sermon.js` (9 KB zero-dependency script).
- **Embed iFrame**: `https://www.sermonindex.net/embed/frame/?theme=dark&topic=prayer`.

---

### 4.7 Hosted Bible Corpus & Expository Commentaries

SermonIndex mirrors the **Free Use Bible API** (AO Labs) under `https://api.sermonindex.net/v2/bible/`.

#### 1. Available Translations Directory: `GET /v2/bible/available_translations.json`
Catalog of 1,253 global translations.
*Fields:* `id`, `name`, `shortName`, `englishName`, `language`, `textDirection` (`ltr`/`rtl`), `numberOfBooks`, `totalNumberOfChapters`, `totalNumberOfVerses`, `sha256`, `listOfBooksApiLink`.

#### 2. Translation Books: `GET /v2/bible/{translation}/books.json`
Books available in that translation with canonical order, chapter counts, and verse totals.

#### 3. Chapter Text & Audio: `GET /v2/bible/{translation}/{bookId}/{chapter}.json`
Complete text of a chapter:
- `chapter.content`: Array of typed content blocks: `"heading"`, `"verse"` (with verse number and text), `"line_break"`.
- `chapter.footnotes`: Textual, cross-reference, and translation footnotes.
- `thisChapterAudioLinks`: Spoken chapter narration (Gilbert, Hays, Souer audio tracks).
- `nextChapterApiLink`, `previousChapterApiLink`: Navigation links to adjacent chapters.

#### 4. Historical Commentaries:
- Catalog: `GET /v2/bible/available_commentaries.json` (6 historical commentaries: Adam Clarke, Jamieson-Fausset-Brown, John Gill, Keil & Delitzsch, Matthew Henry, Tyndale Study Notes).
- Commentary Books: `GET /v2/bible/c/{commentaryId}/books.json`.
- Commentary Profiles: `GET /v2/bible/c/{commentaryId}/profiles.json`.
- Chapter Commentary: `GET /v2/bible/c/{commentaryId}/{bookId}/{chapter}.json` (e.g. `/v2/bible/c/adam-clarke/GEN/1.json`).

---

## 5. Bulk Downloads & Offline Storage Architecture

To process or index the entire catalog, clients must download bulk packages instead of crawling endpoints:

| Package File | Size | Format | Description |
|---|---|---|---|
| `sermonindex.db` | ~172 MB | SQLite 3 | Fully indexed relational database with tables: `sermon`, `speaker`, `topic`, `sermon_topic`, `sermon_scripture` |
| `sermons.json.gz` | ~26.7 MB | JSONL (Gzip) | One sermon JSON object per line. Streamed by [`CatalogSyncWorker`](file:///c:/Users/vzore/AntigravityProjects/Sermons%20App/app/src/main/java/com/example/sermonindex/data/sync/CatalogSyncWorker.kt) into Room FTS4 |
| `topics.json.gz` | ~9.9 MB | JSON (Gzip) | Every topic with its description and scripture links |
| `scripture-index.json.gz` | ~3.6 MB | JSONL (Gzip) | Relational map: scripture reference to sermon IDs |
| `speakers.json.gz` | ~266 KB | JSON (Gzip) | Preachers catalog with bios and portrait links |
| `sermonindex-api.zip` | ~1.41 GB | ZIP | Complete static file tree of `/v2` |
| `sermonindex-bible-api.zip` | ~1.76 GB | ZIP | Complete Bible and commentary JSON corpus |
| `MANIFEST.json` | ~1 KB | JSON | SHA-256 verification hashes, sizes, and build dates |

### Relational Schema of `sermonindex.db`:
- `sermon`: `id`, `slug`, `title`, `speaker`, `duration`, `mp3_url`, `cdn_mp3_url`, `summary`, `has_audio`, `has_transcript`, `description`.
- `speaker`: `slug`, `name`, `bio`, `image`, `sermon_count`.
- `topic`: `slug`, `name`, `description`, `sermon_count`.
- `sermon_topic`: Join table `(sermon_id, topic_slug)`.
- `sermon_scripture`: Relational table `(sermon_id, book_id, chapter, verse)`.

### Offline Kiwix Packages (ZIM):
- OPDS 1.2 Library Catalog: `https://api.sermonindex.net/kiwix/library.xml`
- RSS 2.0 Feed with Enclosures: `https://api.sermonindex.net/kiwix/feed.xml`
- Portable ZIM archives for offline playback in airplane mode or remote mission fields (all text transcripts: 629 MB, preacher-specific bundles: 700 MB to 22.7 GB).

---

## 6. Canonical Book Codes (OSIS Reference Table)

All scripture endpoints mandate 3-character **uppercase OSIS abbreviations**:

| Old Testament | OSIS Code | Chapters | New Testament | OSIS Code | Chapters |
|---|---|---|---|---|---|
| Genesis | `GEN` | 50 | Matthew | `MAT` | 28 |
| Exodus | `EXO` | 40 | Mark | `MRK` | 16 |
| Leviticus | `LEV` | 27 | Luke | `LUK` | 24 |
| Numbers | `NUM` | 36 | John | `JHN` | 21 |
| Deuteronomy | `DEU` | 34 | Acts | `ACT` | 28 |
| Joshua | `JOS` | 24 | Romans | `ROM` | 16 |
| Judges | `JDG` | 21 | 1 Corinthians | `1CO` | 16 |
| Ruth | `RUT` | 4 | 2 Corinthians | `2CO` | 13 |
| 1 Samuel | `1SA` | 31 | Galatians | `GAL` | 6 |
| 2 Samuel | `2SA` | 24 | Ephesians | `EPH` | 6 |
| 1 Kings | `1KI` | 22 | Philippians | `PHP` | 4 |
| 2 Kings | `2KI` | 25 | Colossians | `COL` | 4 |
| 1 Chronicles | `1CH` | 29 | 1 Thessalonians | `1TH` | 5 |
| 2 Chronicles | `2CH` | 36 | 2 Thessalonians | `2TH` | 3 |
| Ezra | `EZR` | 10 | 1 Timothy | `1TI` | 6 |
| Nehemiah | `NEH` | 13 | 2 Timothy | `2TI` | 4 |
| Esther | `EST` | 10 | Titus | `TIT` | 3 |
| Job | `JOB` | 42 | Philemon | `PHM` | 1 |
| Psalms | `PSA` | 150 | Hebrews | `HEB` | 13 |
| Proverbs | `PRO` | 31 | James | `JAS` | 5 |
| Ecclesiastes | `ECC` | 12 | 1 Peter | `1PE` | 5 |
| Song of Solomon | `SNG` | 8 | 2 Peter | `2PE` | 3 |
| Isaiah | `ISA` | 66 | 1 John | `1JN` | 5 |
| Jeremiah | `JER` | 52 | 2 John | `2JN` | 1 |
| Lamentations | `LAM` | 5 | 3 John | `3JN` | 1 |
| Ezekiel | `EZK` | 48 | Jude | `JUD` | 1 |
| Daniel | `DAN` | 12 | Revelation | `REV` | 22 |
| Hosea | `HOS` | 14 | | | |
| Joel | `JOL` | 3 | | | |
| Amos | `AMO` | 9 | | | |
| Obadiah | `OBA` | 1 | | | |
| Jonah | `JON` | 4 | | | |
| Micah | `MIC` | 7 | | | |
| Nahum | `NAM` | 3 | | | |
| Habakkuk | `HAB` | 3 | | | |
| Zephaniah | `ZEP` | 3 | | | |
| Haggai | `HAG` | 2 | | | |
| Zechariah | `ZEC` | 14 | | | |
| Malachi | `MAL` | 4 | | | |

---

## 7. Android Clean Architecture Implementation Guidelines

When designing and extending features in the SermonIndex Android codebase (Kotlin, Jetpack Compose, Clean Architecture, Hilt, Room, Media3), follow these rules:

1. **Retrofit Network Client ([`NetworkClient.kt`](file:///c:/Users/vzore/AntigravityProjects/Sermons%20App/app/src/main/java/com/example/sermonindex/data/network/NetworkClient.kt))**:
   - `Http404EmptyInterceptor`: Safely transforms 404 responses for scripture verse lookups into `{ "sermons": [] }` without throwing unhandled exceptions.
   - Configure a 50 MB OkHttp disk cache (`Cache(cacheDir, CACHE_SIZE)`).
2. **Paging 3 Integration**:
   - For preacher sermons, use [`SpeakerSermonsPagingSource`](file:///c:/Users/vzore/AntigravityProjects/Sermons%20App/app/src/main/java/com/example/sermonindex/ui/speakers/SpeakerDetailViewModel.kt) targeting `/v2/speakers/{slug}/page/{page}` with fixed `pageSize = 50`.
3. **Full-Text Search (Room FTS4)**:
   - Use [`CatalogSyncWorker`](file:///c:/Users/vzore/AntigravityProjects/Sermons%20App/app/src/main/java/com/example/sermonindex/data/sync/CatalogSyncWorker.kt) to stream `sermons.json.gz` (26.7 MB) and populate the virtual `sermon_fts` table with `tokenizer=unicode61` for sub-millisecond offline search.
4. **Periodic Update Checks**:
   - [`CatalogUpdateCheckWorker`](file:///c:/Users/vzore/AntigravityProjects/Sermons%20App/app/src/main/java/com/example/sermonindex/data/sync/CatalogUpdateCheckWorker.kt): Periodically poll `/v2/stats`, compare `generated` against `catalog_metadata.generatedAt`, and notify the user via system notifications when a new catalog is available.
5. **Media3 Playback Engine ([`PlaybackService.kt`](file:///c:/Users/vzore/AntigravityProjects/Sermons%20App/app/src/main/java/com/example/sermonindex/playback/PlaybackService.kt))**:
   - Check local file storage first.
   - If streaming, attempt `sermon.mp3Url` first; upon playback error, automatically switch to `sermon.cdnMp3Url`.
   - Download synchronized WebVTT transcript from `sermon.vttUrl` for Karaoke-style highlighted reading.
6. **Bible Reader & Cross-Linking**:
   - Fetch translation books and chapters via `/v2/bible/...`.
   - Use `thisChapterAudioLinks` with [`PlaybackManager`](file:///c:/Users/vzore/AntigravityProjects/Sermons%20App/app/src/main/java/com/example/sermonindex/playback/PlaybackManager.kt) for spoken audio narration.
   - Cross-link verse markers to `/v2/scripture/{bookId}/{chapter}/{verse}` to surface relevant sermons.
