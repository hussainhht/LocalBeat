# Milestone 5.1 — Android YouTube downloads

## Runtime maintenance

The Maven library/FFmpeg remain 0.18.1. Its bundled extractor reports 2025.11.12; Maven and yt-dlp versions are independent.

YouTubeRuntime installs only upstream's pinned 2026.10.26 Python zipimport artifact after checking SHA-256 e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3. The artifact includes matching EJS (upstream pins 0.8.0). The first YouTube operation performs the check on its existing IO worker. Failed download/checksum/runtime/option checks retain or restore the previous extractor. Interrupted installation restores the backup next time. Requests and updates share a lock; cancellation does not acquire it.

The official wrapper offers YoutubeDL.updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE). Its 0.18.1 implementation does not verify a digest before replacing the extractor, so LocalBeat uses a pinned verified artifact instead. There is no arbitrary channel, runtime binary download, remote EJS execution setting, cookie import, or login bypass.

Python, QuickJS, FFmpeg and ABI packaging require an app release. Updating the extractor/EJS pin also requires a reviewed source change, matching upstream digest, compatibility probe, and live Android testing. A failed check is attempted again after process restart. QuickJS is libqjs.so in applicationInfo.nativeLibraryDir; the wrapper supplies --js-runtimes quickjs:<absolute path> itself. No extra flag is necessary.

Official references:
- https://github.com/yt-dlp/yt-dlp/wiki/EJS
- https://github.com/yt-dlp/yt-dlp/issues/15012
- https://github.com/yausername/youtubedl-android/tree/0.18.1
- https://github.com/yt-dlp/yt-dlp/releases/tag/2026.10.26

## Pipeline changes

Known watch/mobile/share/shorts URL shapes canonicalize to a single video without share parameters. Signed direct-media URLs remain intact. Metadata continues to come from yt-dlp. Audio selection prefers bestaudio, then a combined stream with audio, with --check-formats testing accessibility before transfer. Every retry extracts fresh URLs. FFmpeg conversion, validation, queue, service, Room and publication use the existing pipeline.

Debug builds log bounded sanitized warnings/errors under LocalBeatDownload, with metadata/transfer/conversion/publication stage context. URLs and credential/header lines are removed. Normal users see specific failure categories, never native output.

## Reproduction

Build/install app-debug.apk and app-debug-androidTest.apk, then:

```
adb shell am instrument -w -e liveYouTube true -e class com.example.localbeat.YouTubeRuntimeTest,com.example.localbeat.NativeAudioImportTest com.example.localbeat.test/androidx.test.runner.AndroidJUnitRunner
```

Live tests use Blender's public Big Buck Bunny video aqz-KE-bpKQ through watch, youtu.be and shorts-shaped routes, each as MP3/M4A. The shorts-shaped route tests URL handling of the same permitted video, not an independently verified genuine Short. Each successful case must transfer bytes, complete the queue, publish, appear in MusicRepository, and advance MediaPlayer playback. Rows/files created by tests are removed.

## Evidence collected

Only emulator-5554 (Android 17/API 37, x86_64) was connected. No physical ARM64 phone was available.

Before update:
- Executed yt-dlp: 2025.11.12.
- Executed libqjs.so --help: QuickJS 2025-04-26.
- Metadata succeeded; yt-dlp warned that YouTube forced SABR and skipped formats with missing URLs.
- Transfer failed: ERROR: unable to download video data: HTTP Error 403: Forbidden.
- This reproduces a media-transfer failure, not a URL validation or FFmpeg failure.

After verified update:
- Android logs: active yt-dlp=2026.10.26; bundled EJS=true.
- Verbose execution confirms Python 3.12.11, QuickJS 2025-04-26, FFmpeg/FFprobe 7.1.1 and yt_dlp_ejs 0.8.0.
- Actual extraction logs: [youtube] [jsc:quickjs] Solving JS challenges using quickjs; solver lib/core v0.8.0 loaded from the Python package.
- Deno, Node and Bun are reported unavailable. No additional runtime is needed for this tested flow.
- APK inspection confirms libqjs.so, libpython.so, libffmpeg.so and libffprobe.so for arm64-v8a and x86_64.
- Both application and instrumentation APKs assembled and installed.
- Final :app:assembleDebug, :app:testDebugUnitTest, :app:lintDebug and :app:assembleDebugAndroidTest passed together (offline Gradle run). All five new policy/native/diagnostic regression tests passed.
- All six live cases passed: watch MP3/M4A, youtu.be MP3/M4A, and Shorts-shaped URL MP3/M4A.
- Each case asserted transferred/published size > 1024 bytes, successful queue status, Songs discovery, positive duration and advancing MediaPlayer playback.
- Android instrumentation: OK (3 tests), including native PCM-to-MP3/M4A publication and one intentionally skipped direct-URL smoke test without sampleUrl.
- The initial EJS archive check falsely rejected the official shebang-prefixed ZIP; the fixed check scans past the shebang. Do not interpret the initial EJS=false log as proof that the original bundled executable lacked EJS.
- Physical ARM64 execution and an independently verified genuine Short remain untested.

## Final verification

Final APK instrumentation passed: OK (3 tests), 241 seconds:
1. youtu.be M4A download, MediaStore publication, Songs lookup, MainActivity launch, and advancing playback through LocalBeat AudioPlayer and Media3 PlaybackService.
2. Live direct URL https://download.blender.org/peach/trailer/trailer_400p.ogg through metadata, transfer, MP3 conversion, MediaStore and Songs lookup.
3. Native PCM conversion/publication as MP3 and M4A.

Recorded success: PASS case=1 format=M4A bytes/publish/library/LocalBeat-playback (2026-11-04 00:32:43 device local time).

The earlier six-case matrix passed all watch/share/Shorts-shaped MP3/M4A combinations using MediaPlayer for decoding. The final test uses the application's controller/service instead. An intermediate fixed-delay playback assertion was inconclusive and was replaced by the application playback check with a bounded wait. An initially chosen direct sample returned 404; the final run uses the previously verified trailer URL above and passes.

Build, unit tests (including five new regression tests), and lint passed. Instrumentation test compilation and lint were rerun successfully after refining the playback test. Lint task success does not imply zero warnings; the existing lifecycle lint-registry limitation documented in MILESTONE_5.md remains.

## Files changed

- app/src/main/java/com/example/localbeat/data/model/DownloadItem.kt — trim pasted URL whitespace before validation.
- app/src/main/java/com/example/localbeat/data/repository/DownloadRepository.kt — failure categories and sanitized stage diagnostics.
- app/src/main/java/com/example/localbeat/downloader/AudioDownloader.kt — runtime preparation, accessible audio selection, diagnostics.
- app/src/main/java/com/example/localbeat/downloader/ExtractionPolicy.kt — URL normalization, format policy, native checks and redaction.
- app/src/main/java/com/example/localbeat/downloader/YouTubeRuntime.kt — verified pinned extractor/EJS maintenance and rollback.
- app/src/test/java/com/example/localbeat/YouTubePolicyTest.kt — five regression tests.
- app/src/androidTest/java/com/example/localbeat/YouTubeRuntimeTest.kt — opt-in live Android pipeline and LocalBeat playback verification.
- docs/milestone-5.1-youtube.md — evidence, limitations and maintenance/reproduction instructions.

No commits or pushes were made. Existing unrelated README and nested LocalBeat working-tree changes were preserved.
