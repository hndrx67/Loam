package org.hndrx.loamgallery.model

enum class ThemeMode { System, Light, Dark }
enum class Palette { Blue, Sage, Violet, Rose, Amber }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    val palette: Palette = Palette.Blue,
    val columns: Int = 3,
    val thumbnailSize: Int = 384,
    val memoryCacheMb: Int = 64,
    val prefetch: Boolean = true,
    val lowMemoryThumbnails: Boolean = false,
    val videoThumbnails: Boolean = true,
    val transitions: Boolean = false,
    val watchChanges: Boolean = true,
    val filmstrip: Boolean = true,
) {
    fun sanitized() = copy(
        columns = columns.coerceIn(2, 6),
        thumbnailSize = thumbnailSize.takeIf { it in listOf(192, 384, 768) } ?: 384,
        memoryCacheMb = memoryCacheMb.takeIf { it in listOf(32, 64, 128) } ?: 64,
    )

    fun resetPerformance() = copy(thumbnailSize = 384, memoryCacheMb = 64, prefetch = true,
        lowMemoryThumbnails = false, videoThumbnails = true, transitions = false, watchChanges = true)
}

inline fun <reified T : Enum<T>> enumPreference(value: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback
