package org.hndrx.loamgallery.data

import android.content.SharedPreferences
import androidx.core.content.edit
import org.hndrx.loamgallery.model.*

class SettingsStore(private val prefs: SharedPreferences) {
    fun read(): AppSettings {
        val legacyTheme = if (prefs.contains("dark")) {
            if (prefs.getBoolean("dark", false)) ThemeMode.Dark else ThemeMode.Light
        } else ThemeMode.System
        return AppSettings(
            theme = enumPreference(prefs.getString("theme", null), legacyTheme),
            palette = enumPreference(prefs.getString("palette", null), Palette.Blue),
            columns = prefs.getInt("columns", 3),
            thumbnailSize = prefs.getInt("thumbnailSize", 384),
            memoryCacheMb = prefs.getInt("memoryCacheMb", 64),
            prefetch = prefs.getBoolean("prefetch", true),
            lowMemoryThumbnails = prefs.getBoolean("lowMemoryThumbnails", false),
            videoThumbnails = prefs.getBoolean("videoThumbnails", true),
            transitions = prefs.getBoolean("transitions", false),
            watchChanges = prefs.getBoolean("watchChanges", true),
            filmstrip = prefs.getBoolean("filmstrip", true),
        ).sanitized()
    }

    fun write(settings: AppSettings) = prefs.edit {
        putString("theme", settings.theme.name); putString("palette", settings.palette.name)
        putInt("columns", settings.columns); putInt("thumbnailSize", settings.thumbnailSize)
        putInt("memoryCacheMb", settings.memoryCacheMb); putBoolean("prefetch", settings.prefetch)
        putBoolean("lowMemoryThumbnails", settings.lowMemoryThumbnails)
        putBoolean("videoThumbnails", settings.videoThumbnails); putBoolean("transitions", settings.transitions)
        putBoolean("watchChanges", settings.watchChanges); putBoolean("filmstrip", settings.filmstrip)
    }
}
