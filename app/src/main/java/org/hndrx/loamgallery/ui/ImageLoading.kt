package org.hndrx.loamgallery.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.memory.MemoryCache
import org.hndrx.loamgallery.model.AppSettings

val LocalLoamImages = staticCompositionLocalOf<ImageLoader> { error("Loam image loader is missing") }
val LocalLoamSettings = staticCompositionLocalOf { AppSettings() }

@Composable
fun ImageLoading(settings: AppSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val loader = remember(settings.memoryCacheMb) {
        ImageLoader.Builder(context).memoryCache {
            MemoryCache.Builder().maxSizeBytes(minOf(settings.memoryCacheMb * 1024L * 1024L, Runtime.getRuntime().maxMemory() / 4)).build()
        }.build()
    }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    CompositionLocalProvider(LocalLoamImages provides loader, LocalLoamSettings provides settings, content = content)
}
