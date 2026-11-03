# LocalBeat Milestone 6

## Design and navigation
LocalBeat now has an original crimson and charcoal identity across its Compose UI. The fixed dark theme uses #E73346 as its primary accent, #FF4D60 for highlights, #101014 for the background, and the requested charcoal surface/text/border palette. Dark ink on primary buttons improves label contrast. Typography uses Android's system sans-serif with custom weights, sizes and line heights.

The four primary destinations are Home, Search, Library and Downloads. Each retains its navigation state. Library filters replace the previous permanent text menu, and selected filters scroll into view. Detail screens have Back actions; reselecting a navigation destination returns to its root. Search drafts and scroll state use saveable state. Now Playing opens independently of the navigation chrome.

## Screens
- Home: local-time greeting, waveform branding, favorites feature, actual recently played/added/favorite/most-played carousels, saved playlists, browse shortcuts and honest empty states.
- Library: compact artwork rows, saved sorting and direction, song counts, Play all, Shuffle, favorites and song option sheets.
- Albums: adaptive artwork grid and album details with artwork, metadata, playback and songs.
- Artists: generic artist avatars (no fabricated photos), counts, album carousel and song collection.
- Favorites: heart cover, atmospheric header and shared song/playback controls.
- Playlists: generated covers, creation, add/remove songs, rename/delete confirmation, details and Room-backed membership.
- Search: offline filtering in background work, grouped song/album/artist/playlist results, browse tiles and clear/search keyboard actions.
- Mini Player: floating rounded controller, artwork, title/artist, real progress, animated playback icon and next action.
- Now Playing: atmospheric background, responsive artwork, title/artist/favorite, seek and elapsed/duration, shuffle/previous/play/next/repeat, album information and queue sheet. Dragging keeps a local seek value until committed.
- Downloads: distinct local-video import, URL/paste/metadata flow, format and conversion-quality controls, real task cards, cancellation/retry, history filters, completed-history clearing and open/play.

## Shared components
BeatColors and BeatDimens centralize the palette and key dimensions. Shared components include BeatIcons, SongArtwork, PlaylistCover, SongRow, MusicCard, AlbumCard, ArtistRow, PlaylistCard, PrimaryPlayButton, FavoriteButton, MiniPlayer, DownloadTaskCard, SectionHeader, EmptyState, LoadingState, ErrorState and OptionsSheet.

Animations include destination crossfades, selected icon colors, favorite color/scale feedback, play-button press scaling, play/pause crossfades, Mini Player appearance and Material sheet/dialog transitions. Compose and Material animation APIs retain platform duration-scale behavior.

The Activity uses light system-bar icons over dark edge-to-edge content. Insets are consumed by the scaffold; keyboard-visible layouts protect the input region. Grids adapt to width, the player uses two columns on wide windows, narrow actions wrap and long text truncates or scrolls. Launcher and Android 12+ splash artwork use the original waveform mark.

## Functional integrity
MediaStore remains the music catalog. Playback still uses the existing shared AudioPlayer, MediaController and PlaybackService. Favorites, playlists and listening history retain their existing Room schema and repository logic. The yt-dlp adapter, FFmpeg processor, download service, cancellation/retry, publication and persisted task records are retained. Known percentages remain determinate; unknown conversion/save progress remains indeterminate.

No production sample library or simulated download progress was added. UI tests generate temporary audio, exercise actual MediaStore/Room/Media3 state and remove their media and playlist afterward.

## Files created
Paths below are relative to the project root.
- app/src/main/java/com/example/localbeat/ui/theme/Dimensions.kt
- app/src/main/java/com/example/localbeat/ui/components/BeatIcons.kt
- app/src/main/java/com/example/localbeat/ui/components/MusicComponents.kt
- app/src/main/java/com/example/localbeat/ui/components/DownloadTaskCard.kt
- app/src/main/java/com/example/localbeat/ui/screens/SearchScreen.kt
- app/src/main/res/drawable/ic_localbeat.xml
- app/src/main/res/drawable-anydpi-v26/ic_localbeat.xml
- app/src/main/res/values-v31/themes.xml
- app/src/androidTest/java/com/example/localbeat/Milestone6UiTest.kt
- MILESTONE_6.md

## Files modified
- app/src/main/java/com/example/localbeat/MainActivity.kt
- app/src/main/java/com/example/localbeat/navigation/AppNavigation.kt
- app/src/main/java/com/example/localbeat/navigation/Screen.kt
- app/src/main/java/com/example/localbeat/ui/components/BottomNavBar.kt
- app/src/main/java/com/example/localbeat/ui/components/MiniPlayer.kt
- app/src/main/java/com/example/localbeat/ui/components/SongArtwork.kt
- app/src/main/java/com/example/localbeat/ui/screens/HomeScreen.kt
- app/src/main/java/com/example/localbeat/ui/screens/LibraryScreen.kt
- app/src/main/java/com/example/localbeat/ui/screens/SongsScreen.kt
- app/src/main/java/com/example/localbeat/ui/screens/PlayerScreen.kt
- app/src/main/java/com/example/localbeat/ui/screens/DownloadsScreen.kt
- app/src/main/java/com/example/localbeat/ui/theme/Color.kt
- app/src/main/java/com/example/localbeat/ui/theme/Theme.kt
- app/src/main/java/com/example/localbeat/ui/theme/Type.kt
- app/src/main/AndroidManifest.xml
- app/src/main/res/values/colors.xml
- app/src/main/res/values/themes.xml
- app/src/main/res/drawable/ic_launcher_background.xml
- app/src/main/res/drawable/ic_launcher_foreground.xml
- gradle/libs.versions.toml

## Verification
Baseline assembleDebug passed before implementation. The redesigned debug and optimized release variants assemble, all 13 existing unit tests pass, and lint passes without suppressing checks. The initial transient lint analyzer crash passed on a settled rerun. Existing dependency/lifecycle lint compatibility warnings remain.

The API 37 x86_64 emulator was installed and launched through Android Studio. The connected suite passed its full UI journey before the final accessibility refinements; verification is being repeated on the final source. That journey covers search draft restoration across Activity recreation and tabs, favorite persistence, collection navigation, playlist creation/membership/rename/deletion, actual Media3 play/pause, seeking to 00:30, shuffle and queue access. The native test converts and publishes real MP3 and M4A files through FFmpeg and MediaStore.

Fifteen actual emulator screenshots cover Home, Search, library songs, album grid/detail, artist list/detail, Favorites, playlists, Mini Player, Now Playing and queue. They are exported under app/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/Medium_Phone_API_37.0(AVD) - 17/.

Manual checks at 320dp width and 150% text found and fixed clipped navigation labels and a split MP3 label. Navigation now measures its height from content, and format chips wrap as whole controls. The URL field stays above the keyboard. A real metadata fetch from the public Blender trailer URL returned trailer_400p / Generic, and ACTION_SEND prepopulation was verified on the running Activity. No download was enqueued during this manual preview check. Emulator display size and text scale were restored afterward.

The testing stack remains JUnit4, Robolectric, Compose UI tests and AndroidJUnitRunner. Espresso was updated from 3.5.1 to 3.7.0 and AndroidX test JUnit from 1.1.5 to 1.3.0 after the older input injector failed on API 37.

Commands:
```powershell
.\gradlew.bat build :app:connectedDebugAndroidTest '-Pandroid.enableAdditionalTestOutput=true' --console=plain
```

The UI test captures actual emulator windows to the additionalTestOutputDir instrumentation argument. AGP exports these alongside connected-test output; its [artifact collector implementation](https://android.googlesource.com/platform/tools/base/+/95fd82a56d2c27676620cd25ae6fec291c666977/android-test/android-test-junit-engine/src/main/java/com/android/tools/androidtest/testengine/AndroidDeviceDescriptor.kt) configures collection before APK cleanup. Screenshots are review artifacts, not golden-image comparison tests.

## Remaining validation
Physical devices, a full RTL/font-scale/window-size matrix, Android 24–28 storage/permission behavior, and long background download/cancellation scenarios require further device coverage. The existing opt-in network smoke test runs only when sampleUrl is supplied. Native-pipeline release considerations remain documented in MILESTONE_5.md.

No commit or push was performed. The pre-existing untracked LocalBeat/ nested repository was left alone.
