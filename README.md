# SermonIndex Android App

Native Android application designed for streaming, exploring, and listening to classical sermons, preachers, and scripture-based audio from [SermonIndex](https://sermonindex.net).

## Features

- **Home & Discover**: Quick access to featured, recent, and popular sermons.
- **Speakers Directory**: Browse thousands of classic preachers (e.g. Leonard Ravenhill, A.W. Tozer, Charles Spurgeon) with detailed biographies and complete sermon archives.
- **Scripture Explorer**: Find sermons categorized book-by-book, chapter-by-chapter across the Old and New Testaments.
- **Topics & Series**: Explore sermons organized by doctrinal and topical tags.
- **Persistent Audio Player**:
  - Powered by **Jetpack Media3 (ExoPlayer)** with background playback and lockscreen / notification controls.
  - Fullscreen audio player with playback speed adjustment, seek controls, and scrub bar.
  - Mini-player persistent across screens.
- **My Library**:
  - Saved favorites
  - Playback history & saved resume positions
  - Offline downloads support

## Tech Stack & Architecture

- **Language:** Kotlin 2.x
- **UI Framework:** Jetpack Compose + Material 3
- **Navigation:** Jetpack Navigation 3
- **Audio Engine:** AndroidX Media3 (`ExoPlayer`, `MediaSessionService`)
- **Networking:** Retrofit 2 + OkHttp 4 + `kotlinx.serialization`
- **Image Loading:** Coil 3
- **Local Storage:** Room Database (Favorites, Playback History)
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 / 16 (API 36)

## Getting Started

### Prerequisites

- Android Studio Ladybug (or newer)
- JDK 17
- Android SDK 36

### Building and Running

1. Clone the repository:
   ```bash
   git clone https://github.com/<username>/sermons-app.git
   cd sermons-app
   ```
2. Open the project in Android Studio.
3. Sync Gradle and build the app:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run on an Android emulator or connected device.

## License

This project is open-source. Please see individual libraries for their respective licenses.
