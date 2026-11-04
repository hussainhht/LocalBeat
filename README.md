<div align="center">

# 🎵 LocalBeat

### **Your music. Your rules. Zero ads.**

A sleek, personal, offline-first Android music player with native audio importing and downloading capabilities — wrapped in a dark crimson interface.

[![Platform](https://img.shields.io/badge/Platform-Android_7.0+_(API_24+)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Vibe Coded](https://img.shields.io/badge/Built_With-100%25_AI_Vibe_Coding-E73346?style=flat-square)](#vibe-coding)
[![Status](https://img.shields.io/badge/Status-Active_Development-orange?style=flat-square)](#development-status)

</div>

---

## 📖 The Real Story — Why LocalBeat Exists

Like millions of music lovers, I reached my breaking point with modern music streaming apps.

Every time I opened an app just to hear a song, I was bombarded with unskippable ads, audio interruptions, aggressive subscription pop-ups, clunky social widgets, and cluttered interfaces designed to sell podcasts rather than play music. I didn't want algorithms dictating my taste, and I certainly didn't want advertisements between my favorite tracks. I just wanted to listen to my own music — whenever, wherever, and without interruptions.

So I decided to stop complaining and build my own player: **a clean, lightning-fast, completely ad-free music app.**

As I started working on it, another frustration came to mind: the fragmented workflow of managing local music on Android. Typically, you have to use one questionable browser tool or shady converter app to download or extract an audio file, navigate file managers to find it, and then switch to a separate media player to listen to it.

I asked myself: **Why can't a single, beautiful app do both?**
- Browse and play local music effortlessly.
- Extract soundtracks directly from device videos.
- Download and convert authorized web media straight into your system music library.

That is how **LocalBeat** was born. It is a personal project fueled by genuine annoyance and the desire to build a premium, distraction-free music experience for everyday listening.

---

<a id="vibe-coding"></a>
## 🤖 100% Vibe Coded with AI

> **"LocalBeat is a 100% vibe-coded Android application. The idea, vision, requirements, and direction are mine, while the code was created and iteratively developed with AI coding agents."**  
> — **Hussain Ali** (Creator)

Transparency matters. LocalBeat was not written line-by-line in a traditional manual software engineering cycle. Instead, it was conceived, architected, prompted, evaluated, and steered entirely through **AI-assisted vibe coding**:

- **The Human Role:** Conceived the project idea, established functional requirements, designed the core user flows, prioritized features, directed AI coding agents, evaluated architectural decisions, stress-tested real behaviors on emulators and physical devices, and drove iterative refinements.
- **The AI Role:** Generated boilerplate, wrote Kotlin & Jetpack Compose implementations, integrated AndroidX Media3 / ExoPlayer, configured Room database migrations, built native JNI wrappers for yt-dlp and FFmpeg, and continuously refactored code based on feedback.

Vibe coding made it possible for an independent creator to transform an ambitious vision — an integrated music player with native audio processing, background playback, and MediaStore synchronization — into a working Android codebase.

---

## 🎯 Core Philosophy

LocalBeat is guided by a straightforward set of principles:

- **Zero Ads. Period:** No banners, no interstitial videos, no sponsored playlists, no audio commercials.
- **Offline-First & Local:** Your music library lives on your device. LocalBeat discovers files directly through Android's `MediaStore`, giving you total ownership of your tracks.
- **No Accounts, No Subscriptions:** No logins, no tracking, no monthly fees, and no remote servers collecting your listening habits.
- **One Unified Experience:** Seamlessly bridge the gap between media acquisition and offline playback without relying on third-party file managers.
- **Music-First Interface:** An atmospheric crimson-and-charcoal visual design engineered to make browsing and listening feel tactile and immersive.

> [!NOTE]
> LocalBeat is a personal Android music player and media management utility. It is not a cloud streaming platform and does not offer hosted cloud music catalogs.

---

## ✨ Features

### 🎧 Music Playback & Audio Engine
- **AndroidX Media3 / ExoPlayer:** Low-latency, rock-solid audio playback supporting popular audio formats (MP3, M4A, AAC, FLAC, OGG, WAV).
- **Background MediaSessionService:** Uninterrupted playback when navigating outside the app, switching tasks, or locking your screen.
- **Floating Mini Player:** Persistent bottom controller featuring album artwork, real-time progress indicators, play/pause toggles, and track skipping.
- **Now Playing Screen:** Immersive full-screen playback with dynamic atmospheric wine-crimson backdrop, draggable seek bar with live scrub preview, repeat (Off / All / One), shuffle, and instant favorite toggles.
- **Live Playback Queue:** Interactive queue sheet to inspect upcoming tracks and navigate playlist order on the fly.
- **System Media Controls:** Complete lock screen media controls, Bluetooth AVRCP metadata sync, and notification drawer media sessions.

### 📚 Music Library & Smart Organization
- **Home Hub:** Contextual time-aware greeting, quick filter chips (*All music*, *Favorites*, *Playlists*, *Recently added*), and a featured "Always on repeat" favorites hero banner.
- **Songs Browser:** Instant access to all local audio with customizable sorting (by title, artist, date added, duration) and ascending/descending order.
- **Albums & Artists:** Responsive album art grid with detailed album track listings and dedicated artist discographies.
- **Custom Playlists:** Create, rename, edit, and delete custom playlists backed by local SQLite persistence.
- **Favorites:** One-tap heart toggle on any track for instant addition to your personal favorites collection.
- **Instant Offline Search:** Fast, zero-lag in-memory and database search filtering songs, artists, albums, and playlists simultaneously.
- **Listening Statistics:** Automated tracking of *Recently Played* and *Most Played* tracks to resurface your top rotations.

### 📥 Audio Downloading & Media Imports
- **Pre-Download Media Inspector:** Preview track titles, thumbnails, duration, uploader, reported audio codecs, and stream bitrates before initiating any transfer.
- **Supported Online Media Extraction:** Download audio from supported online sources via embedded yt-dlp.
- **Flexible Audio Conversion:** Automatic stream-copying for compatible AAC/M4A containers or high-quality conversion to MP3/M4A via bundled FFmpeg with selectable bitrates.
- **Local Video-to-Audio Extraction:** Extract clean soundtrack audio directly from local video files stored on your Android device via the system document picker (`video/*`).
- **Android Share Sheet Integration:** Share URLs (`text/plain`) or video files (`video/*`) directly from other apps into LocalBeat via Android's native `ACTION_SEND` intent filter.
- **Resilient Background Service:** Long-running downloads run in a dedicated foreground service (`dataSync` and `mediaProcessing`), surviving screen timeouts.
- **Persistent Task Queue:** Downloads are saved in Room database across application restarts, supporting real-time progress, cancellation, and manual retry.
- **Automated MediaStore Publishing:** Completed audio files are published directly into Android's public `Music/LocalBeat` directory with proper metadata tags, automatically triggering a library refresh.

### 🎨 Visual Identity & Interface
- **Crimson & Charcoal Design:** Built from the ground up with custom design tokens (`#E73346` Crimson, `#FF4D60` Highlight, `#101014` Dark Charcoal Background, and `#491D2B` Wine Gradients).
- **Jetpack Compose Native:** 100% declarative UI built with Material 3 components, buttery 60fps animations, crossfades, and touch feedback.
- **Adaptive Edge-to-Edge:** Native support for modern Android window insets, navigation bars, display cutouts, and soft keyboards.

---

## 🛠️ Technology Stack

LocalBeat leverages modern Android development libraries and verified native integrations:

| Component | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | `2.2.10` | Idiomatic, type-safe development |
| **Build System** | Gradle (AGP) | `9.5.0` | Modern Android build tooling |
| **Target Platforms** | Android SDK | Min `24`, Compile/Target `37` | Coverage from Android 7.0 to Android 15+ |
| **UI Framework** | Jetpack Compose | BOM `2026.02.01` | Declarative UI and adaptive layouts |
| **Design System** | Material 3 | Compose M3 | Modern Material design components |
| **Audio Engine** | AndroidX Media3 | `1.11.1` | ExoPlayer playback core & MediaSession |
| **Local Database** | Room | `2.8.4` (via KSP `2.3.4`) | Playlists, favorites, stats, and download tasks |
| **Media Extraction** | youtubedl-android | `0.18.1` | Native JNI wrapper for yt-dlp |
| **Audio Processing** | FFmpeg Android | `0.18.1` | Audio extraction, stream remuxing & transcoding |
| **Image Loading** | Coil | `2.7.0` | Asynchronous image and thumbnail loading |
| **Concurrency** | Kotlin Coroutines & Flow | — | Reactive state streams (`StateFlow`) |
| **Catalog Provider** | Android MediaStore | Platform API | System-wide audio discovery and storage |
| **Testing** | JUnit4, Espresso, Robolectric | `4.13.2` / `3.7.0` | Comprehensive unit and UI instrumentation |

---

## 🏗️ How LocalBeat Works

LocalBeat follows an offline-first, reactive architecture powered by Android Jetpack architecture components:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        LocalBeat User Interface                        │
│             (Jetpack Compose • Material 3 • MVVM Screens)              │
└──────────────────┬──────────────────────────────────┬──────────────────┘
                   │                                  │
       [Plays Audio / Controls]             [Manages Downloads / Imports]
                   ▼                                  ▼
┌─────────────────────────────────────┐    ┌─────────────────────────────────────┐
│             AudioPlayer             │    │          DownloadRepository         │
│     (Media3 ExoPlayer Session)      │    │       (DownloadService / Room)      │
└──────────────────┬──────────────────┘    └──────────────────┬──────────────────┘
                   │                                  │
       [Reads Discovered Tracks]            [Fetches / Transcodes / Saves]
                   ▼                                  ▼
┌─────────────────────────────────────┐    ┌─────────────────────────────────────┐
│          MusicRepository            │    │     AudioDownloader & Processor     │
│        (Android MediaStore)         │    │      (yt-dlp JNI • FFmpeg CLI)      │
└──────────────────▲──────────────────┘    └──────────────────┬──────────────────┘
                   │                                  │
                   └────────── [Auto-Publishes To] ────┘
                         Music/LocalBeat Directory
```

### 1. Local Music Flow
- The application queries the system `MediaStore.Audio.Media` provider.
- Tracks are mapped into clean `Song` domain models without modifying the original media files.
- The `AudioPlayer` ViewModel interacts with a long-running `PlaybackService` (`MediaSessionService`) that coordinates playback states across the UI, lock screen, and system notification.

### 2. User Data & Playlists Flow
- User-specific modifications (favorites, custom playlists, play counts, and listening history) are persisted locally in an on-device SQLite database via **Room**.
- Room state changes emit reactive Kotlin `Flow`s, keeping the UI instantly updated.

### 3. Media Download & Processing Pipeline
- **Metadata Preview:** The input URL is passed to `yt-dlp` in single-JSON dump mode off the main thread to retrieve media information, available formats, and bitrates.
- **Background Execution:** `DownloadService` executes one queued download item at a time under an active foreground notification with data synchronization flags.
- **Conversion & Remuxing:** If the source stream matches the target container (e.g. AAC into M4A), a stream copy is performed. Otherwise, FFmpeg converts the audio stream into the requested format (MP3 or M4A) at the user's selected bitrate.
- **MediaStore Publication:** The finalized audio file is written into Android's public `Music/LocalBeat` directory using scoped storage (`IS_PENDING` mechanism on Android 10+ or direct storage permissions on Android 7–9), followed by a library refresh notification.

### 4. Local Video Extraction Pipeline
- When a video file is selected via Android's Storage Access Framework (`video/*`), `MediaExtractor` and FFmpeg extract the embedded audio stream into an M4A or MP3 track, tagging it and saving it directly into your music library.

---

<a id="development-status"></a>
## 🚦 Development Status

LocalBeat is an actively developed personal project. We believe in complete honesty regarding the state of every feature:

| Feature Area | Current Status | Notes |
| :--- | :---: | :--- |
| **Local Music Playback** | ✅ **Verified** | Stable playback via AndroidX Media3 / ExoPlayer with MediaSession. |
| **Background Playback & Notification** | ✅ **Verified** | Lock screen, Bluetooth, and notification media controls functional. |
| **Library Management** | ✅ **Verified** | Songs, albums, artists, favorites, and playlists backed by Room. |
| **Offline Search & Filtering** | ✅ **Verified** | Grouped results across songs, albums, artists, and playlists. |
| **Theme & UI Animations** | ✅ **Verified** | Dark Crimson palette, responsive layouts, and crossfade transitions. |
| **Local Video Audio Extraction** | ✅ **Verified** | Extracts audio tracks from local device video files into `Music/LocalBeat`. |
| **Direct Media URL Downloads** | ✅ **Verified** | Successfully verified with direct public media streams. |
| **YouTube URL Extraction** | ✅ **Manually tested** | The creator confirmed successful audio downloading on a physical Android phone. Automated ARM64 testing has not been performed. |

> [!NOTE]
> YouTube audio downloading has passed the creator's manual test on a physical Android phone. This development preview does not guarantee that every video or device will work; automated ARM64 testing remains outstanding.

---

## 📸 Screenshots

> *UI previews and device screenshots of the Crimson interface (Home, Now Playing, Library, and Downloads) are being captured from the latest build and will be added here shortly.*

---

## 🚀 Getting Started (For Developers)

LocalBeat is currently distributed as an open-source Android Studio project. There are no official pre-built APK releases on Google Play at this time.

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer recommended.
- **JDK:** Version 11 or Version 17.
- **Android SDK:** Platform API 37 (Minimum API 24 / Android 7.0).
- **Target Device:** An Android emulator or a physical device running Android 7.0 (Nougat) or higher.

### Installation & Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/hussainhht/LocalBeat.git
   cd LocalBeat
   ```

2. **Open the project in Android Studio:**
   - Launch Android Studio and select **Open**.
   - Navigate to the cloned `LocalBeat` folder and click **OK**.
   - Allow Android Studio to sync the project with Gradle files.

3. **Build the Debug APK via command line:**
   - On **Windows (PowerShell/CMD):**
     ```cmd
     .\gradlew.bat assembleDebug
     ```
   - On **macOS / Linux:**
     ```bash
     ./gradlew assembleDebug
     ```

4. **Run on an Android device or emulator:**
   - In Android Studio, select your connected device or virtual device (AVD).
   - Click the green **Run (Play)** button, or execute:
     ```bash
     ./gradlew installDebug
     ```

### Required Permissions
Upon launching the application, LocalBeat will request necessary system permissions based on your Android version:
- **Android 13+ (API 33+):** `READ_MEDIA_AUDIO` (to scan your local songs) and `POST_NOTIFICATIONS` (for playback controls and download progress).
- **Android 10–12 (API 29–32):** `READ_EXTERNAL_STORAGE` (to discover audio files via MediaStore).
- **Android 7.0–9 (API 24–28):** `WRITE_EXTERNAL_STORAGE` (required on legacy versions to publish downloaded/extracted tracks into the public Music folder).

---

## 🗺️ Roadmap

Here is what is planned for future milestones:

- [ ] **Expand YouTube Download Testing:** Add automated ARM64 coverage and test more devices and video formats.
- [ ] **Embedded Lyrics Support:** Parse synchronized and unsynchronized lyrics from file ID3 tags and LRC files.
- [ ] **Equalizer & Audio Effects:** Implement a built-in 5-band or 10-band graphic equalizer with bass boost and virtualizer.
- [ ] **Sleep Timer:** Automatic playback fade-out and sleep timer for bedtime listening.
- [ ] **Batch Media Import:** Support multiple URL queuing and multi-video batch audio extraction.
- [ ] **Tag Editor:** Built-in ID3 tag and album artwork editor for local songs.
- [ ] **Pre-built Release Builds:** Signed release APKs and GitHub release assets.

---

## ⚖️ Legal & Licensing Transparency

LocalBeat is developed as a personal open-source project. Before considering binary redistribution or publishing, please note the following licensing obligations:

- **GPL-3.0 Dependencies:** LocalBeat incorporates `youtubedl-android`, which is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. Any distributed combined binaries must comply with GPL-3.0 source availability and notice requirements.
- **FFmpeg Licensing:** The bundled FFmpeg binaries are licensed under the **LGPL v2.1+** or **GPL v3.0** depending on the specific build flags and enabled components.
- **Notice & Source Distribution:** A complete review of bundled third-party licenses, copyright notices, and corresponding source distributions must be completed prior to any public release or binary distribution.
- **Authorized Use Only:** LocalBeat is designed exclusively for playing user-owned local audio and downloading media that the user is legally authorized to download. LocalBeat does not circumvent DRM, bypass digital rights protections, or evade access controls. Users are solely responsible for ensuring compliance with applicable copyright laws and third-party terms of service.

---

## 👨‍💻 Creator

<div align="center">

**Hussain Ali**  
*Creator, Vision & Direction*  

[![GitHub](https://img.shields.io/badge/GitHub-hussainhht-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/hussainhht)

*Conceived and directed by Hussain Ali — vibe-coded in partnership with AI.*

</div>

---

<div align="center">
<sub>Built with passion, coffee, and AI prompt engineering. No ads. Just music.</sub>
</div>
