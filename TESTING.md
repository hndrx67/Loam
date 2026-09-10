# Loam 1.0.4 beta — device validation

JVM unit tests and lint run through Gradle. The following checks require a test device/profile and are not covered by those checks. Use synthetic images and videos for deletion tests.

## Viewer and appearance

- Swipe through an album and Favorites: the filmstrip must show only the opened collection, highlight the current URI, and follow the page.
- Scroll the filmstrip independently and tap nearby and distant image/video thumbnails. Check first/last items and collections containing one item.
- Zoom a photo, tap another thumbnail, and return. Verify zoom gestures and page swipes still work.
- Tap a photo to hide/show controls. Verify no overlap between the filmstrip, action buttons, and video controls in portrait and landscape.
- Switch Light, Dark, and System modes. Change the device theme while Loam is open; System should follow it and explicit modes should not.
- Exercise all five palettes, rotate/recreate the activity, and restart the process. Appearance and viewer/grid preferences should persist.
- Verify the launcher name and header say Loam and Settings reports version 1.0.4 beta.

## Delete and restore — use disposable media

- Cancel Loam's delete-choice dialog. Nothing should change.
- Choose the bin, then cancel Android's confirmation. The original remains accessible.
- Approve moving a photo and a video to the bin. They should disappear from the library/viewer and appear in Settings → Recycle bin.
- Restore each item. Native trash restores the original; older-device local copies restore into the Loam album.
- Choose permanent deletion, read the warning, and cancel. Repeat with approval using disposable media only.
- Permanently remove a disposable bin item, checking that another entry remains intact.
- Rotate while Android's consent dialog is showing. Complete or cancel it and verify Loam returns to a usable state without issuing the operation twice.
- Interrupt the process before confirmation, during an older-device copy, and after original deletion. Check that an original or a complete recovery copy remains. Never use irreplaceable media for this test.
- On Android 8–9, deny and then grant write permission during a media operation.
- On Android 10, test media-location permission denial/grant and recoverable write consent on media created by another app. Verify original bytes/metadata after restore.
- On Android 11+, check trash expiry labels, items trashed by another app, and devices that prevent reading a trashed preview.
- Revoke full access or select a smaller media set while the app is backgrounded. The gallery/bin must only show accessible native media; private recovery copies must remain available.
- Simulate low storage during copy/restore. Failure must preserve the original or recovery copy and show an error.

## Settings and performance

- Install as an update over the earlier build and check migration of favorites, grid columns, and the old theme preference.
- Change thumbnail resolution and cache budget, then scroll a large synthetic library and revisit items. Profile memory, decode activity, and frame timing.
- Disable video thumbnails: grid/filmstrip frames should become play placeholders while playback remains available.
- Toggle preloading, reduced-memory thumbnails, and transitions; confirm visible/request behavior, including stopping preloads in the background.
- Disable live updates, modify media elsewhere, and verify manual refresh/refresh on return still works.
- Clear image memory cache and reset performance settings. Photos, bin entries, favorites, theme, palette, columns, and filmstrip preference must remain intact.
- Check Settings, the bin, empty/error screens, and confirmation dialogs at large font sizes and in landscape.

## Existing functionality

- Denied/limited/full media access, search, album/system Back, scroll restoration, and activity recreation.
- Background or swipe away from playing video: audio stops; returning preserves position without autoplay.
- Open a temporarily granted external URI without broad library permission, and test an expired URI.
- Share an image and a video to another installed app and verify read access.

Never clear a personal device's app data, revoke its permissions, or delete its media as part of these tests without the owner's agreement.
