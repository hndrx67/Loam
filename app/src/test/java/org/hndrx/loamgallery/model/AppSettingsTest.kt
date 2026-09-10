package org.hndrx.loamgallery.model

import org.junit.Assert.*
import org.junit.Test

class AppSettingsTest {
    @Test fun corruptOrOldPreferencesFallBackToSupportedValues() {
        val settings = AppSettings(columns = -4, thumbnailSize = Int.MAX_VALUE, memoryCacheMb = -1).sanitized()
        assertEquals(2, settings.columns)
        assertEquals(384, settings.thumbnailSize)
        assertEquals(64, settings.memoryCacheMb)
        assertEquals(ThemeMode.System, enumPreference("unknown", ThemeMode.System))
        assertEquals(Palette.Blue, enumPreference(null, Palette.Blue))
    }

    @Test fun performanceResetPreservesAppearanceAndViewerPreferences() {
        val custom = AppSettings(theme = ThemeMode.Dark, palette = Palette.Sage, columns = 6, filmstrip = false,
            thumbnailSize = 192, memoryCacheMb = 128, prefetch = false, lowMemoryThumbnails = true,
            videoThumbnails = false, transitions = true, watchChanges = false)
        val reset = custom.resetPerformance()
        assertEquals(AppSettings(theme = ThemeMode.Dark, palette = Palette.Sage, columns = 6, filmstrip = false), reset)
    }

    @Test fun defaultPreferencesFollowTheDeviceAndShowFilmstrip() {
        assertEquals(ThemeMode.System, AppSettings().theme)
        assertTrue(AppSettings().filmstrip)
        assertEquals(ThemeMode.Light, enumPreference("Light", ThemeMode.System))
    }
}
