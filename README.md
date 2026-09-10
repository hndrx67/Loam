# Loam — 1.0.4 beta

A local photo and video gallery built with Kotlin and Jetpack Compose, with a Samsung Gallery-inspired interface.

## In this beta

- Loam branding in the launcher and gallery header; version code 104
- Pictures, Albums, Favorites, and a dedicated Settings tab
- A viewer filmstrip with image/video thumbnails, current-item highlight, automatic positioning, and tap-to-jump navigation
- Persistent Light, Dark, and System appearance modes; Blue, Sage, Violet, Rose, and Amber color schemes throughout the app
- Delete choices: move to the recycle bin or permanently delete, with explicit confirmation
- Settings → Recycle bin: restore individual items or permanently remove them
- Experimental performance controls: thumbnail resolution, memory-cache budget, adjacent-photo preloading, reduced-memory thumbnails, video previews, transitions, live updates, cache clearing, and reset to defaults

The gallery also includes date sections, file/album search, adaptive album cards, a 2–6 column grid, local favorites, bounded zoom, non-destructive image rotation, Media3 playback, sharing, details, and external-file viewing.

## Recycle-bin behavior

On Android 11 and later, Loam uses Android's native trash, restore, and permanent-delete confirmation requests. The bin shows accessible trashed media, including items trashed by other apps. Android controls automatic expiry; Loam displays the provider's expiry date when available. Some devices do not allow previews of trashed files until restored.

On Android 8–10, Loam saves a verified recovery copy in private, non-backed-up storage before removing the original. Copy failures do not authorize deletion. Journals and checksums support interrupted-action recovery. Copies stay until restored or deleted; clearing Loam's app data or uninstalling the app removes them. Restored copies go to Pictures/Loam or Movies/Loam. Android 8–9 asks for write access when needed. Android 10 may ask for media-location access to preserve original metadata, followed by the system's approval to modify another app's media.

Deleting an externally opened file is not offered unless it is opened from Loam's local library. Permanent deletion has no undo. Loam never empties the bin automatically.

## Performance controls

Fast / Balanced / Detailed previews request 192 / 384 / 768 pixel thumbnails; full-screen images retain display-sized decoding. The image-memory budget is capped at a quarter of the app's available heap. Adjacent-photo preloading prepares only the previous and next image and is cancelled when the viewer becomes inactive. Disabling video previews skips thumbnail frame decoding, but does not disable playback.

Clear image memory cache only clears decoded image cache entries. It does not remove photos, videos, favorites, settings, or recycle-bin copies. Performance controls are experimental and need profiling on the target device; they do not guarantee lag-free scrolling. Library metadata is still loaded in memory, so paging remains future work for very large libraries.

## Build and verification

Use JDK 17 or 21 with Android SDK 36. Select a compatible Gradle JDK in Android Studio; the bundled JDK 25 is not suitable for this Gradle 8.13 build.

```text
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On Windows use `gradlew.bat`. APK: `app/build/outputs/apk/debug/app-debug.apk`. Reports: `app/build/reports`.

JVM tests cover permissions, zoom bounds, duration formatting, preference validation/reset, and recovery-copy persistence, interruption, corruption, retry, and removal. See [TESTING.md](TESTING.md) for device-only validation.

## Architecture

`MainActivity` manages permission/system-consent launchers, external intents, and media observation. `GalleryViewModel` owns loading, preferences, viewer collections, and pending media operations. `MediaRepository` queries media off the main thread. `MediaActions` handles platform operations and `TrashArchive` maintains older-device recovery copies. UI is split between `LoamApp`, `Viewer`, `SettingsScreen`, and `RecycleBinScreen`; `ImageLoading` supplies one shared configurable image loader.

Preferences from the earlier dark-theme toggle and grid settings are migrated. Package ID remains `org.hndrx.loamgallery`, so installing an update preserves app data. Dates use MediaStore's date added. Favorites are local to Loam. Editing, multi-select operations, cloud sync, and private albums are not included in this beta.
