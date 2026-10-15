# SermonIndex Android App

[![Version](https://img.shields.io/badge/version-1.0.2-blue.svg)](PROGRESS.md)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](app/build.gradle.kts)
[![Architecture](https://img.shields.io/badge/architecture-Clean%20%2B%20MAD-orange.svg)](ARCHITECTURE.md)

Native Android application designed for streaming, exploring, and studying classical sermons, preachers, and scripture-based audio from [SermonIndex](https://sermonindex.net).

---

## ✨ Features

- **Home & Discover**: Quick access to featured hero sermons, top preaching themes, and a "Continue Listening" shelf with resume positions.
- **Speakers Directory**: Browse and search over 2,200 classic preachers (e.g. Leonard Ravenhill, A.W. Tozer, Charles Spurgeon, David Wilkerson) with detailed biographies and paginated archives.
- **Scripture Explorer**: Find sermons mapped to all 66 books of the Bible, complete with chapter selector and verse sermon density heatmaps.
- **Topics & Themes**: Explore sermons organized across 14,000+ doctrinal and topical tags, correlated with Berean Standard Bible references.
- **Persistent Audio Engine (Jetpack Media3)**:
  - Background playback service with lockscreen & media notification controls.
  - **Automatic CDN Failover**: Seamless fallback to backup CDN streams when the primary audio host is unavailable.
  - Fullscreen player with speed adjustment (0.75x–2.0x), 15s rewind / 30s fast-forward, chapter outlines, transcripts, and notes.
  - Dismissible floating Mini-player persistent across navigation tabs.
- **My Library**:
  - Saved favorites with one-tap toggle.
  - Listening history with progress tracking and clear options.
  - **Offline Catalog & Full-Text Search (FTS4)**: Background GZIP sync via WorkManager indexing 64,000+ sermons for instant offline prefix and keyword search.

---

## 🛠 Tech Stack & Architecture

The application strictly adheres to **Clean Architecture** (Presentation → Domain → Data) and **Modern Android Development (MAD)** standards:

- **Language:** Kotlin 2.x
- **UI Framework:** Jetpack Compose + Material 3 (Edge-to-Edge)
- **Navigation:** Jetpack Navigation 3 (`NavDisplay`, `NavKey`)
- **Dependency Injection:** Dagger Hilt
- **Audio Engine:** AndroidX Media3 (`ExoPlayer`, `MediaSessionService`)
- **Networking:** Retrofit 2 + OkHttp 4 + `kotlinx.serialization`
- **Background Work:** AndroidX WorkManager (GZIP streaming sync)
- **Local Storage & Search:** Room Database + SQLite FTS4 virtual tables
- **Image Loading:** Coil 3
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 / 16 (API 36)

For detailed architectural guidelines and package conventions, see [ARCHITECTURE.md](ARCHITECTURE.md).  
For the current roadmap and implementation status, see [PROGRESS.md](PROGRESS.md).

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug (or newer)
- JDK 17+
- Android SDK 36

### Building and Running

1. Clone the repository:
   ```bash
   git clone https://github.com/vzorenko/sermons-app.git
   cd sermons-app
   ```
2. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Deploy to an emulator or connected device via Android Studio or ADB.

---

## 📄 License

This project is open-source. Please see individual libraries for their respective licenses.
