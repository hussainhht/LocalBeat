# LocalBeat Milestone 5 implementation notes

## Dependencies and native integration
- Maven Central: io.github.junkfood02.youtubedl-android:library:0.18.1 and :ffmpeg:0.18.1.
- Verified upstream README and Kotlin implementations of YoutubeDL.execute, init, destroyProcessById, YoutubeDLRequest, and FFmpeg.init. No FFmpegKit or custom extractor.
- The FFmpeg module initializes/extracts dependencies; AudioProcessor invokes its packaged libffmpeg.so and libffprobe.so through ProcessBuilder argument arrays. It does not expose an execute API.
- Minimum API 24, compile/target API 37. Extracted native packaging; packaged FFmpeg ABIs verified in build outputs: arm64-v8a, armeabi-v7a, x86, x86_64.
- Upstream 0.18.0 release reports 16 KB page support. Physical ARM and 16 KB device verification remains required. Module version is not the FFmpeg executable version.
- Upstream references: https://github.com/yausername/youtubedl-android and https://github.com/yausername/youtubedl-android/releases/tag/0.18.0

## Licensing implications
The wrapper repository is GPL-3.0. Distribution of a combined application needs a GPL-compatible licensing and corresponding-source delivery arrangement, license texts and copyright notices. This change does not relicense existing user code or complete a release compliance package.
FFmpeg's default license is LGPL 2.1+, but optional GPL components change the applicable license. Do not assume this bundled binary is LGPL-only: audit its build configuration and bundled dependencies and provide their matching sources/notices before distributing.
References: https://ffmpeg.org/legal.html and https://www.gnu.org/licenses/gpl-faq.en.html .
No proprietary distribution clearance is claimed.

## Architecture and behavior
The existing DownloadItem, DownloadRepositoryImpl, AudioDownloaderImpl and DownloadsScreen replace their mock implementations.
DownloadsViewModel fetches real extractor JSON off the main thread. Preview includes title, optional artwork/duration/uploader, extractor platform, reported formats and source bitrates. Uploader is not assigned as music artist.
Output choices are BEST (compatible AAC/M4A), M4A and MP3. Automatic M4A stream-copies AAC; other combinations convert with FFmpeg. Requested bitrates are explicitly conversion settings, not source-quality claims. Artwork is preview-only; title, actual artist and album metadata are passed to FFmpeg.
DownloadService is a separate, direct user-started foreground service with dataSync and, on API 35+, mediaProcessing types. It drains one Room queue task at a time. It holds a bounded partial wake lock, provides an ongoing notification with queue cancellation, and handles platform timeouts. It does not use PlaybackService or JobScheduler, so Android 16 JobScheduler quotas do not govern this queue. Foreground-service time limits and system termination still apply.
Tasks persist across restart. Interrupted active/queued tasks become FAILED with a retry explanation and pending MediaStore cleanup; they do not silently redownload after a restart or force-stop.
Callbacks supply real transfer percentages when available. Unknown processing progress is indeterminate. Byte counters are nullable and currently not reported by the wrapper adapter. Progress writes are throttled to at most about one per 750 ms.
Transfers select the native yt-dlp downloader. Cancel kills the registered yt-dlp process and direct FFprobe/FFmpeg process; a watcher covers cancellation during native process registration. Retry uses the same record and starts from scratch. Clearing completed history does not delete music.
Final publication is a short non-cancellable save operation; the UI disables cancellation during SAVING.

## Storage and library
All working files are app-cache owned. FFprobe detects source audio and codec, including source containers Android cannot inspect. MediaExtractor validates the finished audio track and positive output duration before publication.
API 29+: insert MediaStore.Audio with RELATIVE_PATH Music/LocalBeat and IS_PENDING, stream-copy data, clear pending only after copy. Failed insert/copy rolls back. The pending URI is recorded for interruption recovery.
API 24–28: request WRITE_EXTERNAL_STORAGE, derive the public Music directory with platform APIs, use a LocalBeat folder and MediaScannerConnection. No broad storage-management permission.
Names are normalized, limited in length and include a UUID suffix to avoid collisions. File extension and MIME match MP3 or M4A.
A successful publication increments a repository signal observed by navigation, which calls the existing SongsViewModel.refresh. MediaStore remains the sole music catalog.

## Local input and sharing
OpenDocument(video/*) grants access to a content URI; persistable grants are taken when available. Metadata is read with MediaMetadataRetriever. The service stream-copies the URI into its own temporary file and reuses AudioProcessor and MediaStorePublisher. Original video is never changed.
ACTION_SEND text/plain prepopulates an HTTP(S) URL; video/* accepts a content URI. onNewIntent and recreation are handled. The user must preview and explicitly start every import. Shared providers may not offer durable grants; retry can require choosing the file again.
Missing, audio-less, protected, unsupported or corrupt media produces a sanitized failure. URLs are never interpolated into shell commands. No cookies, authentication import, DRM bypass or raw extractor error logging is added.

## Room
LibraryDatabase version 2 adds DownloadItem and DownloadDao with explicit MIGRATION_1_2. Existing Favorite, LocalPlaylist, PlaylistSong and PlayStat tables remain intact. Schema exported at app/schemas/com.example.localbeat.data.library.LibraryDatabase/2.json.
Records contain identity, source, title/artwork, output settings, status/stage/progress/optional bytes, error, final/pending URI, timestamps and local-source flag.

## Files changed for this milestone
Modified:
- app/build.gradle.kts
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/example/localbeat/MainActivity.kt
- app/src/main/java/com/example/localbeat/data/model/DownloadItem.kt
- app/src/main/java/com/example/localbeat/data/repository/DownloadRepository.kt
- app/src/main/java/com/example/localbeat/data/library/LibraryDatabase.kt
- app/src/main/java/com/example/localbeat/downloader/AudioDownloader.kt
- app/src/main/java/com/example/localbeat/navigation/AppNavigation.kt
- app/src/main/java/com/example/localbeat/ui/screens/DownloadsScreen.kt

Created:
- app/src/main/java/com/example/localbeat/downloader/DownloadDao.kt
- app/src/main/java/com/example/localbeat/downloader/DownloadService.kt
- app/src/main/java/com/example/localbeat/downloader/MediaStorePublisher.kt
- app/src/main/java/com/example/localbeat/ui/screens/DownloadsViewModel.kt
- app/schemas/com.example.localbeat.data.library.LibraryDatabase/2.json
- app/src/test/java/com/example/localbeat/DownloadPersistenceTest.kt
- app/src/test/java/com/example/localbeat/DownloadPipelineTest.kt
- app/src/androidTest/java/com/example/localbeat/NativeAudioImportTest.kt
- MILESTONE_5.md

Pre-existing uncommitted library and playback work was preserved. No commit or push.

## Verification
Final verification passed together: :app:assembleDebug, :app:testDebugUnitTest (13 tests), :app:lintDebug and :app:connectedDebugAndroidTest (3 tests, including the opt-in network test). Gradle reported BUILD SUCCESSFUL in 3m 6s. Five new tests cover URL/name validation, persisted queue reopen, cancellation, retry, actual repository stage transitions with fake native adapters, and Room migration preserving library data.
Lint has warnings including obsolete dependency suggestions and an incompatible lifecycle custom lint registry; its affected checks are skipped. A passing lint task does not mean zero warnings.
API 37 x86_64 emulator installation, app launch, Downloads navigation and complete URL input verified. The opt-in public Blender trailer test passed end to end: actual yt-dlp metadata, actual network transfer, real MP3 conversion, successful MediaStore publication, and discovery through MusicRepositoryImpl. Test-created music was removed afterward. Source: https://download.blender.org/peach/trailer/trailer_400p.ogg . The initial direct-media parser and Android source-inspection failures were fixed before the successful final run.
Native instrumented conversion/publication test passed on API 37 x86_64: real MP3 and M4A outputs were validated, published to MediaStore and found by the existing Songs query. The first native run exposed a missing libc++ library search path; the verified fix initializes both packages and includes Python, FFmpeg and native library paths. Additional release verification: Android 24–28 permissions/scanning, ARM ABIs, 16 KB pages, SAF video providers, incoming share grants/recreation, long background operation, platform timeouts, low storage, actual native cancellation and additional site-specific extraction. The foreground-service lifecycle, picker/share flows, and native process cancellation were compiled and/or fake-tested, not exercised end to end on device. Background survival after force-stop is not promised.
