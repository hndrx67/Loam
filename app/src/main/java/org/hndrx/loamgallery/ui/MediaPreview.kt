package org.hndrx.loamgallery.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.size.Precision
import org.hndrx.loamgallery.R

@Composable
fun MediaPreview(uri: Uri, description: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit, backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant, thumbnail: Boolean = true, isVideo: Boolean = uri.pathSegments.contains("video"), onLoadedSize: (Int, Int) -> Unit = { _, _ -> }) {
    val settings = LocalLoamSettings.current
    val loader = LocalLoamImages.current
    val context = LocalContext.current
    val request = remember(uri, settings.thumbnailSize, settings.lowMemoryThumbnails, settings.transitions, thumbnail) {
        ImageRequest.Builder(context).data(uri).crossfade(if (settings.transitions) 150 else 0).apply {
            if (thumbnail) {
                size(settings.thumbnailSize).precision(Precision.INEXACT)
                allowRgb565(settings.lowMemoryThumbnails)
                allowHardware(!settings.lowMemoryThumbnails)
                memoryCacheKeyExtra("loam_low_memory", settings.lowMemoryThumbnails.toString())
            }
        }.build()
    }
    var failed by remember(uri) { mutableStateOf(false) }
    var loading by remember(uri) { mutableStateOf(true) }
    Box(modifier.background(backgroundColor), contentAlignment = Alignment.Center) {
        if (thumbnail && isVideo && !settings.videoThumbnails) {
            Icon(Icons.Default.PlayArrow, description, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Box
        }
        if (loading || failed) Icon(if (failed) Icons.Default.BrokenImage else Icons.Default.Photo,
            if (failed) stringResource(R.string.image_unavailable) else null,
            Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        AsyncImage(model = request, imageLoader = loader, contentDescription = description, modifier = Modifier.fillMaxSize(), contentScale = contentScale,
            onLoading = { loading = true; failed = false },
            onSuccess = { loading = false; failed = false; onLoadedSize(it.result.image.width, it.result.image.height) },
            onError = { loading = false; failed = true })
    }
}
