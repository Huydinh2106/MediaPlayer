# MediaPlayer Android Project

A modern, high-performance media player for Android built with Jetpack Compose and Android Media3.

## Features

- **Media Discovery**: Automatically scans local storage for audio and video files using `MediaStore`.
- **High-Performance Playback**: Powered by **Android Media3 (ExoPlayer)** for seamless audio and video streaming.
- **Background Playback**: Implements `MediaSessionService` to keep media playing when the app is minimized or the screen is locked.
- **System Integration**:
    - **Notification Controls**: Standard media controls (Play/Pause, Skip, Repeat) in the notification drawer.
    - **Lockscreen Controls**: Full playback control and metadata display on the device lockscreen.
    - **Hardware Buttons**: Supports media buttons from Bluetooth headsets and external peripherals.
- **Rich Metadata**: Displays detailed information including title, artist, album, year, size, and duration.
- **Intelligent Thumbnails**: Uses **Coil** to extract frames from videos and display them as thumbnails, with beautiful vector fallbacks for audio.
- **History Tracking**: Keeps a persistent "Recently Played" list using a **Room Database**.
- **Organization**:
    - **Tabbed Interface**: Separate views for **Home** (Recent), **Library** (Audio/Video), and **Settings**.
    - **Search**: Real-time filtering of media items by title or artist.
    - **Advanced Sorting**: Sort by Name, Size, Date, Duration, Artist, or Album with **Ascending/Descending** toggles.
- **Customizable Playback**:
    - **Playlist Navigation**: Automatically loads the current filtered list as a playlist for easy Skip Next/Previous.
    - **Repeat Modes**: Cycle through Off, Repeat One, and Repeat All.
- **Responsive UI**: Fully declarative UI built with **Jetpack Compose** and **Material 3**.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Playback Engine**: Media3 / ExoPlayer & MediaSession
- **Persistence**: 
    - **Database**: Room (for playback history)
    - **Preferences**: Jetpack DataStore (for user settings)
- **Image Loading**: Coil (with Video support)
- **Navigation**: Jetpack Navigation Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Build System**: Gradle Kotlin DSL with Version Catalogs (libs.versions.toml) and KSP

## Project Structure

- `app/src/main/java/com/example/mediaplayer/`
    - `data/`: Contains `MediaFile` models, `MediaStoreRepository`, `SettingsRepository`, and Room configuration (`AppDatabase`).
    - `service/`: Contains `PlaybackService` (MediaSessionService) for background playback.
    - `ui/`: Compose screens (`HomeScreen`, `MediaListScreen`, `PlayerScreen`, `SettingsScreen`) and shared components.
    - `viewmodel/`: `MediaViewModel` managing the `MediaController` link to the service and handling UI state.
    - `MainActivity.kt`: Entry point with navigation host, lifecycle observation, and permission handling.
    - `MediaPlayerApplication.kt`: Custom application class for initializing Coil's `ImageLoader`.

## Setup & Requirements

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Build System**: Android Studio Narwhal 3 (2025.1.3) compatible, AGP 8.13.0.
- **Java Version**: 21

### Permissions
The app requests the following permissions:
- `READ_EXTERNAL_STORAGE` (Legacy storage access)
- `READ_MEDIA_AUDIO` / `READ_MEDIA_VIDEO` (Android 13+ media access)
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (Required for background play)

## Usage

1. **Permissions**: On first launch, grant media access permissions.
2. **Browsing**: Navigate between **Home**, **Library**, and **Settings** using the bottom bar.
3. **Sorting & Filtering**: In the Library, use the search bar to filter and the icons to change sort type and direction.
4. **Playback**: Tap any item to play. Use the in-app player controls or system notification/lockscreen controls to manage playback.
5. **Background Play**: Toggle "Background Playback" in the Settings tab to control if music stops when the app is minimized.
