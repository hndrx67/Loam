**Loam Gallery 1.0.7 Beta**

This prerelease adds multi-selection, custom albums, album sorting, and a draggable gallery scrollbar. Thumbnail preloading can now be cancelled and resumed, and Loam has a new launcher icon.

**Changelog**

- **Resumable thumbnail preload:** Moved Start Thumbnail Preload into Settings → Performance II, marked Experimental and Danger. Cancel an active run without losing completed thumbnails. Starting again skips saved thumbnails at the selected quality, including after reopening the app. New pictures are included only when you manually start another run.
- **Multi-selection:** Long-press pictures, videos, or albums to begin selecting. Select multiple items or use Select all, then add their media to a Loam album or move it to the recycle bin. Android requests approval where required.
- **Create albums:** Create named albums from the Albums page or while adding selected media. Loam albums persist across app restarts and organize media inside the app; original files stay in place.
- **Album sorting:** Sort by name A–Z, name Z–A, newest pictures, or most items. Your sorting preference is saved.
- **Gallery scrollbar:** A draggable scrollbar appears on the right while scrolling through media and albums. Enable or disable it in Settings → Features → Gallery scrollbar.
- **Updated launcher icon:** Uses the new Loam Design D artwork with adaptive padding and a cream background for launcher masks.
- **Selection handling:** Selection survives screen rotation. Selected Loam albums are removed only after their media deletion succeeds.

**Beta notes**

- Thumbnail preloading is experimental and can use significant storage and battery with large libraries. Cached thumbnails reduce repeated image loading but do not guarantee lag-free scrolling.
- Android may remove cached thumbnails to reclaim storage. Clear image cache also removes them; run preloading again to rebuild the cache.
- Custom albums are local to Loam, rather than device folders shared with other gallery apps.
- Device testing is still needed for scrolling, launcher appearance, and Android media-permission flows across different devices.

**Installation and feedback**

Download `LoamGallery1.0.7beta.apk` from this release’s assets and install it on Android 8.0 or newer.

Please report issues with your device model, Android version, reproduction steps, and screenshots when helpful. For performance issues, include the approximate library size and thumbnail settings.

Suggested GitHub release title: `Loam Gallery 1.0.7 Beta`

Suggested tag: `v1.0.7-beta`

