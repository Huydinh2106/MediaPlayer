# MediaPlayer Android Project

A modern, high-performance media player for Android built with Jetpack Compose and Android Media3.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3), with `AndroidView` interop for the Media3 `PlayerView`
- **Playback Engine**: Media3 1.10.1 — ExoPlayer, MediaSession, MediaSessionService, MediaController
- **Persistence**:
    - **Database**: Room (playback history, custom albums)
    - **Preferences**: Jetpack DataStore (user settings)
- **Image Loading**: Coil (with `VideoFrameDecoder` for video thumbnails)
- **Navigation**: Jetpack Navigation Compose
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines + `StateFlow`
- **Build System**: Gradle Kotlin DSL with Version Catalogs (`libs.versions.toml`) and KSP

## Architecture Overview

```
UI (Compose screens)
   │  collects StateFlow / sends user actions
   ▼
MediaViewModel ────────────────► MediaController (Media3)
   │                                   │ IPC (binder)
   │                                   ▼
   │                             PlaybackService (MediaSessionService)
   │                                   │
   │                                   ├── ExoPlayer  (actual playback)
   │                                   └── MediaSession (notification, lockscreen,
   │                                        Bluetooth/headset buttons, other apps)
   ▼
Repositories: MediaStoreRepository (device media), Room DAOs (history, albums),
              SettingsRepository (DataStore preferences)
```

Playback does **not** run inside the Activity. The `ExoPlayer` instance lives in
`PlaybackService`, and the UI talks to it through a `MediaController` that is connected
asynchronously in `MediaViewModel`. Because the `MediaController` implements the Media3
`Player` interface, the Compose UI and the `PlayerView` can use it exactly as if it were
the player itself, while every command actually travels to the service. This is what
makes background playback and system integrations work with a single code path.

## Features & How They Work

### 1. Media Discovery (`data/MediaStoreRepository.kt`)

- Scans the device for audio and video with two `ContentResolver.query()` calls against
  `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` and `MediaStore.Video.Media.EXTERNAL_CONTENT_URI`.
- Reads a projection of columns (`_ID`, `TITLE`, `ARTIST`, `ALBUM`, `YEAR`, `DURATION`,
  `SIZE`, `DATE_MODIFIED`) and maps every row to a `MediaFile` model, building a playable
  `content://` URI with `ContentUris.withAppendedId()`.
- Queries run on `Dispatchers.IO` inside `suspend` functions so the UI thread is never blocked.
- `MediaViewModel.loadMedia()` triggers the scan (called after permissions are granted and on
  screen refresh) and stores the results in `MutableStateFlow`s.

### 2. Permissions Handling (`MainActivity.kt`)

- On Android 13+ (`TIRAMISU`) requests the granular `READ_MEDIA_AUDIO` and
  `READ_MEDIA_VIDEO` permissions; on older versions it falls back to `READ_EXTERNAL_STORAGE`.
- Uses the Compose Activity Result API (`rememberLauncherForActivityResult` with
  `RequestMultiplePermissions`). The navigation host and bottom bar are only shown once
  permission is granted.

### 3. Playback with ExoPlayer (`service/PlaybackService.kt`, `viewmodel/MediaViewModel.kt`)

- `PlaybackService` extends `MediaSessionService`. In `onCreate()` it builds one `ExoPlayer`
  and wraps it in a `MediaSession`, which is handed to any controller via `onGetSession()`.
- The player is built with `setMaxSeekToPreviousPositionMs(Long.MAX_VALUE)` so that a
  "previous" command **always** jumps to the previous item in the playlist. Without this,
  ExoPlayer's default `seekToPrevious()` restarts the current item once more than
  3 seconds have been played — the lockscreen and Bluetooth "previous" buttons route
  through `seekToPrevious()`, so this setting keeps every surface consistent.
- `MediaViewModel` creates a `SessionToken` for the service and connects a
  `MediaController` asynchronously (`MediaController.Builder(...).buildAsync()`). When the
  future resolves, the controller is published through `StateFlow<Player?>` and the UI
  recomposes with live controls.
- `playMedia(mediaFile, playlist)` converts the currently visible (filtered/sorted) list
  into Media3 `MediaItem`s — including `MediaMetadata` (title, artist, album) that the
  notification and lockscreen display — sets them on the controller, seeks to the tapped
  item's index, then calls `prepare()` and `play()`.

### 4. Playlist Navigation (Previous / Next)

- Because `playMedia()` loads the whole visible list as the player's playlist, skipping is a
  playlist operation, not a file reload.
- The in-app control bar uses `seekToPreviousMediaItem()` / `seekToNextMediaItem()`, which
  move strictly between playlist entries (respecting shuffle order and repeat mode).
- The system surfaces (notification, lockscreen, headsets) do the same — see feature 12.

### 5. Repeat Modes & Shuffle (`MediaViewModel.kt`, `ui/PlayerScreen.kt`)

- `toggleRepeatMode()` cycles `Player.REPEAT_MODE_OFF → ONE → ALL` on the controller;
  `toggleShuffleMode()` flips `shuffleModeEnabled`. Both are plain Media3 `Player` APIs, so
  the ExoPlayer in the service applies them to its playlist ordering.
- The ViewModel registers a `Player.Listener` (`onRepeatModeChanged`,
  `onShuffleModeEnabledChanged`) and mirrors the values into `StateFlow`s, so the button
  highlighting always reflects the *actual* player state — even if it was changed from
  another controller.
- In the UI the active state is tinted with the Material 3 primary color, the inactive
  state is dimmed (38% alpha), and the repeat icon switches between `Repeat` and `RepeatOne`.

### 6. Player Screen UI (`ui/PlayerScreen.kt`) — redesigned

The player screen is a full-bleed, black-background `Box` stacking two layers:

1. **`PlayerView` (Media3 UI)** rendered through Compose's `AndroidView` interop. It draws
   the video surface (or artwork for audio), the seek/time bar, the timer text, the
   settings (playback speed/quality) button, and the fullscreen button, and it owns the
   auto-hide behavior of all controls.
2. **A custom Compose control bar** — a rounded, semi-transparent dark pill overlaid at
   `Alignment.BottomCenter`, floating just above the `PlayerView`'s bottom bar (the row
   with the timer and the settings button). It contains, in order:
   **Repeat · Previous · Play/Pause · Next · Shuffle**.

Implementation details:

- **Default center buttons removed**: `setShowPreviousButton(false)` and
  `setShowNextButton(false)` hide the `PlayerView`'s built-in big center previous/next
  buttons. Playlist navigation lives only in the custom control bar, whose previous button
  calls `seekToPreviousMediaItem()` (not `seekToPrevious()`), and next calls
  `seekToNextMediaItem()`.
- **Synchronized auto-hide**: the screen registers a
  `PlayerView.ControllerVisibilityListener`. Whenever the `PlayerView` controller shows or
  hides (tap on the video, auto-hide timeout during playback), the Compose control bar
  fades in/out with it via `AnimatedVisibility` + `fadeIn()`/`fadeOut()`. The custom bar
  therefore sits in the same "position" (the auto-hiding controller layer) as the settings
  button and timer, and disappears while media is playing.
- **Staying visible while interacting**: every button in the custom bar also calls
  `playerView.showController()`, which resets the `PlayerView`'s hide timeout, so the
  controls don't vanish mid-interaction.
- **Live play/pause icon**: a `Player.Listener.onIsPlayingChanged` callback (registered in
  a `DisposableEffect` and removed on dispose) drives the Play/Pause icon state.

### 7. Fullscreen Mode (`ui/PlayerScreen.kt`) — new

- `PlayerView.setFullscreenButtonClickListener { ... }` is set, which makes the
  `PlayerView` display its built-in fullscreen toggle button in the bottom control bar
  (next to the settings button) and reports enter/exit clicks to the listener.
- The fullscreen flag is held in `rememberSaveable` state. A `LaunchedEffect` reacts to it
  using `WindowInsetsControllerCompat`:
    - **Enter**: `hide(WindowInsetsCompat.Type.systemBars())` with
      `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` — status and navigation bars disappear
      (immersive mode) and can be peeked with a swipe. Because `MainActivity` uses
      `enableEdgeToEdge()` and the Scaffold consumes window insets, the content
      automatically expands to the whole screen.
    - **Exit**: `show(systemBars())` restores the bars.
- The system back gesture is intercepted with `BackHandler` while fullscreen: the first
  back press exits fullscreen (and syncs the `PlayerView` icon via
  `setFullscreenButtonState(false)`) instead of leaving the screen. A `DisposableEffect`
  also restores the system bars if the user navigates away while still in fullscreen.
- The app's own bottom navigation bar is already hidden on the player route (it is only
  shown for the `home`, `list`, and `settings` destinations).

### 8. Background Playback (`service/PlaybackService.kt`, `MainActivity.kt`)

- `PlaybackService` is declared in the manifest with
  `foregroundServiceType="mediaPlayback"` and the `MediaSessionService` intent filter.
  While playing, Media3 automatically promotes the service to a foreground service with a
  media notification, so playback survives the app being minimized or the screen locking.
- The "Background Playback" toggle (Settings tab) is persisted in DataStore. `MainScreen`
  observes the Activity lifecycle with a `LifecycleEventObserver`; on `ON_STOP`, if the
  toggle is off, it pauses the player — playback then stops when the app leaves the
  foreground, while the service keeps running for a quick resume.
- `onTaskRemoved()` stops the service if nothing is playing (player paused or playlist
  empty) so no orphan notification is left after swiping the app away.

### 9. Notification & Lockscreen Controls (`service/PlaybackService.kt`) — updated

- Media3's `MediaSessionService` posts a `MediaStyle` notification automatically through
  its `DefaultMediaNotificationProvider`; no manual `NotificationCompat` code is needed.
  The notification shows the current item's `MediaMetadata` (title/artist) plus exactly
  three action buttons: **Previous media item · Play/Pause · Next media item** — the
  provider wires the prev/next actions to `COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM` /
  `COMMAND_SEEK_TO_NEXT_MEDIA_ITEM`, i.e. real playlist navigation.
- On the lockscreen and in the system media output switcher (Android 13+ media controls),
  the buttons are derived from the `MediaSession`'s platform `PlaybackState`. Those route
  through `seekToPrevious()`/`seekToNext()`; thanks to
  `setMaxSeekToPreviousPositionMs(Long.MAX_VALUE)` on the player (feature 3), "previous"
  there also always skips to the previous media item, matching the notification.
- `MediaSession.Callback.onConnect()` explicitly adds the seek-to-previous/next (item)
  commands plus `COMMAND_SET_REPEAT_MODE` / `COMMAND_SET_SHUFFLE_MODE` to the available
  player commands, so system surfaces and the in-app controller may use them.
- The same `MediaSession` also handles hardware media keys from Bluetooth headsets and
  other peripherals.

### 10. Recently Played History (`data/RecentMedia.kt`, `data/AppDatabase.kt`)

- Every call to `playMedia()` inserts a `RecentMedia` row (media id, URI, timestamp, type)
  into a Room database (`media_database`, schema v2, destructive migration fallback).
- `RecentMediaDao.getRecentMedia()` returns a `Flow`, which the ViewModel `combine`s with
  the scanned audio/video lists to resolve ids back to full `MediaFile` objects. The Home
  tab collects this as state, so the "Recently Played" list updates reactively.

### 11. Custom Albums (`data/Album.kt`, `ui/AlbumDetailScreen.kt`, `ui/AddToAlbumDialog.kt`)

- Modeled with two Room entities: `Album` and an `AlbumMediaCrossRef` join table
  (albumId ↔ mediaId), enabling many-to-many relations between albums and media files.
- Users can create/delete albums, and add/remove any audio or video file via the
  `AddToAlbumDialog`. `getAlbumWithMedia(albumId)` combines the cross-references with the
  scanned media lists to produce the album's contents as a `Flow<List<MediaFile>>`.
- Tapping an item inside an album plays it with the album's contents as the playlist.

### 12. Search & Sorting (`viewmodel/MediaViewModel.kt`, `ui/MediaListScreen.kt`)

- The Library lists are exposed as `filteredAudioFiles` / `filteredVideoFiles`:
  `combine(files, searchQuery, sortType, sortOrder)` recomputes reactively whenever any
  input changes — no manual refresh needed.
- Search matches title or artist, case-insensitively. Sorting supports Name, Size, Date,
  Duration, Artist, Album, each with an Ascending/Descending toggle.
- Because playback always uses the currently filtered list as the playlist, Previous/Next
  navigate within exactly what the user sees.

### 13. Thumbnails (`MediaPlayerApplication.kt`, `ui/CommonUi.kt`)

- The application class implements Coil's `ImageLoaderFactory` and registers a
  `VideoFrameDecoder`, so `AsyncImage` composables can render a frame extracted from any
  video URI as its thumbnail, with crossfade. Audio items fall back to vector icons.

### 14. Settings (`data/SettingsRepository.kt`, `ui/SettingsScreen.kt`)

- Preferences are stored in Jetpack DataStore (`preferencesDataStore("settings")`) and
  exposed as `Flow`s — currently the Background Playback switch. DataStore writes are
  transactional `suspend` calls (`dataStore.edit`), safe from the UI thread.

### 15. Navigation & App Shell (`MainActivity.kt`)

- A single-activity app: `NavHost` with routes `home`, `list`, `settings`, `player`, and
  `album_detail/{albumId}` (typed `Long` nav argument).
- A Material 3 `NavigationBar` is shown only on the three top-level tabs; `player` and
  `album_detail` render full-screen. Tab switching uses `popUpTo` + `saveState`/
  `restoreState` + `launchSingleTop` so each tab keeps its scroll/UI state.
- `enableEdgeToEdge()` lets content draw behind the system bars, which the fullscreen
  feature (feature 7) relies on.

## What Changed in the Latest Update

- **Removed** the `PlayerView`'s default big center previous/next buttons
  (`setShowPreviousButton(false)` / `setShowNextButton(false)`).
- **Kept** previous/next in the custom control bar (repeat · previous · play/pause ·
  next · shuffle); the previous button now calls `seekToPreviousMediaItem()` instead of
  `seekToPrevious()` (and next calls `seekToNextMediaItem()`).
- **Moved** the custom control bar into the auto-hiding controller layer: it is overlaid
  on the player at the same position as the settings button and timer row, and it hides
  and shows together with them while media is playing (synced through
  `ControllerVisibilityListener` + `AnimatedVisibility`).
- **Added** a fullscreen button (native `PlayerView` button via
  `setFullscreenButtonClickListener`) with immersive system-bar hiding, back-press
  handling, and state restoration.
- **Notification / lockscreen controls** now consistently expose **previous media ·
  play/pause · next media** on every surface via
  `setMaxSeekToPreviousPositionMs(Long.MAX_VALUE)`.
- **UI refresh** of the player screen: full-bleed black playback surface and a rounded,
  semi-transparent floating control bar that reads well over both video and audio.

## Project Structure

- `app/src/main/java/com/example/mediaplayer/`
    - `data/`: `MediaFile` models, `MediaStoreRepository`, `SettingsRepository`, Room
      entities/DAOs (`RecentMedia`, `Album`) and `AppDatabase`.
    - `service/`: `PlaybackService` (MediaSessionService) hosting ExoPlayer + MediaSession.
    - `ui/`: Compose screens (`HomeScreen`, `MediaListScreen`, `PlayerScreen`,
      `AlbumDetailScreen`, `SettingsScreen`), dialogs, and shared components.
    - `viewmodel/`: `MediaViewModel` — owns the `MediaController` connection and all UI state.
    - `MainActivity.kt`: entry point with navigation host, lifecycle observation, and
      permission handling.
    - `MediaPlayerApplication.kt`: application class configuring Coil's `ImageLoader`.

## Setup & Requirements

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Build System**: Android Studio Narwhal 3 (2025.1.3) compatible, AGP 8.13.0
- **Java Version**: 21

### Permissions

- `READ_EXTERNAL_STORAGE` (legacy storage access, Android 12 and below)
- `READ_MEDIA_AUDIO` / `READ_MEDIA_VIDEO` (Android 13+ granular media access)
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (background playback)

## Usage

1. **Permissions**: on first launch, grant media access.
2. **Browsing**: switch between **Home**, **Library**, and **Settings** with the bottom bar.
3. **Sorting & Filtering**: in the Library, use the search bar and the sort type/direction icons.
4. **Playback**: tap any item to play it with the visible list as the playlist.
5. **Player controls**: tap the video to show/hide the controls; use the floating bar for
   repeat, previous/next track, play/pause, and shuffle; use the bottom row for seeking,
   playback settings, and **fullscreen**.
6. **Fullscreen**: tap the fullscreen button to go immersive; press back or tap it again to exit.
7. **System controls**: manage playback from the notification or lockscreen — previous
   media, play/pause, next media.
8. **Background Play**: toggle "Background Playback" in Settings to choose whether music
   keeps playing when the app is minimized.
