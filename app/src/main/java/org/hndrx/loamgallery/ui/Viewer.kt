package org.hndrx.loamgallery.ui

import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.request.ImageRequest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.hndrx.loamgallery.R
import org.hndrx.loamgallery.model.MediaAsset
import org.hndrx.loamgallery.model.MediaAction
import org.hndrx.loamgallery.model.panLimit
import kotlin.math.min

@Composable
fun Viewer(media: List<MediaAsset>, initialUri: String, favorites: Set<String>, changed: (MediaAsset) -> Unit, toggleFavorite: ((Long) -> Unit)?, delete: ((MediaAsset, MediaAction) -> Unit)? = null, operationBusy: Boolean = false, close: () -> Unit) {
    val context = LocalContext.current
    val canvas = MaterialTheme.colorScheme.background
    val foreground = MaterialTheme.colorScheme.onSurface
    val settings = LocalLoamSettings.current
    val imageLoader = LocalLoamImages.current
    val scope = rememberCoroutineScope()
    val windowSize = LocalWindowInfo.current.containerSize
    val preloadWidth = windowSize.width.coerceAtLeast(1)
    val preloadHeight = windowSize.height.coerceAtLeast(1)
    val shareTitle = stringResource(R.string.share_media)
    val pager = rememberPagerState(initialPage = media.indexOfFirst { it.uri.toString() == initialUri }.coerceAtLeast(0)) { media.size }
    var controls by rememberSaveable { mutableStateOf(true) }
    var details by rememberSaveable { mutableStateOf(false) }
    var zoomed by remember { mutableStateOf(false) }
    var deletingUri by rememberSaveable { mutableStateOf<String?>(null) }
    val item = media.getOrNull(pager.currentPage)
    var rotation by rememberSaveable(item?.uri?.toString()) { mutableIntStateOf(0) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    BackHandler { close() }
    LaunchedEffect(pager.currentPage, media) {
        media.getOrNull(pager.currentPage)?.let(changed)
    }
    LaunchedEffect(pager.currentPage) { zoomed = false }
    LaunchedEffect(pager.settledPage, media, settings.prefetch, imageLoader, lifecycleState, preloadWidth, preloadHeight) {
        if (settings.prefetch && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) coroutineScope {
            listOf(pager.settledPage - 1, pager.settledPage + 1).mapNotNull(media::getOrNull).filterNot { it.isVideo }.forEach { neighbor ->
                launch { imageLoader.execute(ImageRequest.Builder(context).data(neighbor.uri).size(preloadWidth, preloadHeight).build()) }
            }
        }
    }
    Box(Modifier.fillMaxSize().background(canvas)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), key = { media[it].uri.toString() }, userScrollEnabled = !zoomed) { page ->
            val asset = media[page]
            val contentModifier = if (controls) Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(top = 64.dp, bottom = if (settings.filmstrip) 154.dp else 84.dp) else Modifier.fillMaxSize()
            if (asset.isVideo) {
                var position by rememberSaveable(asset.uri.toString()) { mutableLongStateOf(0L) }
                // Removing an inactive player releases its decoder and audio immediately.
                if (pager.settledPage == page && !pager.isScrollInProgress && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
                    Video(asset, position, { position = it }, contentModifier)
                } else MediaPreview(asset.uri, asset.name, contentModifier, backgroundColor = canvas, isVideo = true)
            } else ZoomImage(asset, if (pager.currentPage == page) rotation else 0,
                onZoom = { if (pager.currentPage == page) zoomed = it }, tap = { controls = !controls }, modifier = contentModifier, active = pager.currentPage == page)
        }
        if (controls && item != null) {
            Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).background(canvas).statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = foreground) }
                Column(Modifier.weight(1f)) {
                    Text(item.name, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.page_count, pager.currentPage + 1, media.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = {
                    try { context.startActivity(Intent.createChooser(shareIntent(item), shareTitle)) }
                    catch (_: Exception) { Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show() }
                }) { Icon(Icons.Default.Share, stringResource(R.string.share), tint = foreground) }
            }
            Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(canvas).navigationBarsPadding()) {
            if (operationBusy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.operation_busy), Modifier.padding(horizontal = 16.dp), color = foreground, style = MaterialTheme.typography.labelSmall)
            }
            if (settings.filmstrip) ViewerFilmstrip(media, pager.currentPage) { index ->
                scope.launch {
                    if (index != pager.currentPage) {
                        zoomed = false
                        if (settings.transitions) pager.animateScrollToPage(index) else pager.scrollToPage(index)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (toggleFavorite != null) {
                    val favorite = item.id.toString() in favorites
                    ViewerAction(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        stringResource(R.string.favorite), stringResource(if (favorite) R.string.unfavorite else R.string.favorite), if (favorite) MaterialTheme.colorScheme.primary else foreground) { toggleFavorite(item.id) }
                }
                if (!item.isVideo) ViewerAction(Icons.AutoMirrored.Filled.RotateRight, stringResource(R.string.rotate)) { rotation = (rotation + 90) % 360 }
                if (delete != null) ViewerAction(Icons.Default.DeleteOutline, stringResource(R.string.delete), enabled = !operationBusy) { deletingUri = item.uri.toString() }
                ViewerAction(Icons.Default.Info, stringResource(R.string.details)) { details = true }
            }
            }
        }
        if (details && item != null) AlertDialog(onDismissRequest = { details = false },
            confirmButton = { TextButton(onClick = { details = false }) { Text(stringResource(R.string.close)) } },
            title = { Text(stringResource(R.string.media_details)) }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.name)
                    Text(stringResource(R.string.media_type, item.mimeType))
                    if (item.width > 0 && item.height > 0) Text(stringResource(R.string.media_dimensions, item.width, item.height))
                    if (item.size > 0) Text(stringResource(R.string.media_size, Formatter.formatFileSize(context, item.size)))
                    Text(stringResource(R.string.media_album, item.bucketName))
                    if (item.isVideo && item.duration > 0) Text(stringResource(R.string.media_duration, formatDuration(item.duration)))
                }
            })
        val deleting = media.firstOrNull { it.uri.toString() == deletingUri }
        if (deleting != null && delete != null) DeletionDialog(deleting, dismiss = { deletingUri = null }, confirm = { action -> deletingUri = null; delete(deleting, action) })
    }
}

@Composable
private fun ViewerAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, description: String = label, tint: Color = MaterialTheme.colorScheme.onSurface, enabled: Boolean = true, click: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = click, enabled = enabled) { Icon(icon, description, tint = if (enabled) tint else Color.Gray) }
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ZoomImage(item: MediaAsset, rotation: Int, onZoom: (Boolean) -> Unit, tap: () -> Unit, modifier: Modifier = Modifier, active: Boolean = true) {
    var scale by remember(item.uri, rotation, active) { mutableFloatStateOf(1f) }
    var offset by remember(item.uri, rotation, active) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember(item.uri) { mutableStateOf(IntSize(item.width, item.height)) }
    val latestTap by rememberUpdatedState(tap)
    val swapped = rotation % 180 != 0
    val imageWidth = (if (swapped) imageSize.height else imageSize.width).takeIf { it > 0 }?.toFloat() ?: viewport.width.toFloat()
    val imageHeight = (if (swapped) imageSize.width else imageSize.height).takeIf { it > 0 }?.toFloat() ?: viewport.height.toFloat()
    val fit = if (imageWidth > 0 && imageHeight > 0) min(viewport.width / imageWidth, viewport.height / imageHeight) else 1f
    fun bounded(value: Offset): Offset {
        val x = panLimit(viewport.width.toFloat(), imageWidth * fit, scale)
        val y = panLimit(viewport.height.toFloat(), imageHeight * fit, scale)
        return Offset(value.x.coerceIn(-x, x), value.y.coerceIn(-y, y))
    }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = bounded(offset + pan)
    }
    LaunchedEffect(scale, rotation, active) { if (active) onZoom(scale > 1f) }
    LaunchedEffect(viewport, imageWidth, imageHeight) { offset = bounded(offset) }
    BoxWithConstraints(modifier.clipToBounds().onSizeChanged { viewport = it }
        .pointerInput(item.uri, rotation, active) { detectTapGestures(onTap = { latestTap() }, onDoubleTap = {
            scale = if (scale > 1f) 1f else 2.5f
            offset = Offset.Zero
        }) }
        .transformable(transform, canPan = { scale > 1f }), contentAlignment = Alignment.Center) {
        MediaPreview(item.uri, item.name, Modifier.requiredSize(if (swapped) maxHeight else maxWidth, if (swapped) maxWidth else maxHeight)
            .graphicsLayer { rotationZ = rotation.toFloat(); scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }, backgroundColor = MaterialTheme.colorScheme.background, thumbnail = false,
            onLoadedSize = { width, height -> if (width > 0 && height > 0) imageSize = IntSize(width, height) })
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun Video(item: MediaAsset, position: Long, savePosition: (Long) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    var failed by remember(item.uri) { mutableStateOf(false) }
    val latestSave by rememberUpdatedState(savePosition)
    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setHandleAudioBecomingNoisy(true)
            setMediaItem(MediaItem.fromUri(item.uri))
            seekTo(position)
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener { override fun onPlayerError(error: PlaybackException) { failed = true } }
        player.addListener(listener)
        onDispose { latestSave(player.currentPosition); player.removeListener(listener); player.release() }
    }
    Box(modifier) {
        AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true } },
            update = { it.player = player }, modifier = Modifier.fillMaxSize())
        if (failed) Column(Modifier.align(Alignment.Center).background(Color.Black).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.video_failed), color = Color.White)
            TextButton(onClick = { failed = false; player.prepare() }) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun ViewerFilmstrip(media: List<MediaAsset>, current: Int, jump: (Int) -> Unit) {
    val transitions = LocalLoamSettings.current.transitions
    val state = rememberLazyListState()
    var width by remember { mutableIntStateOf(0) }
    val thumbnailWidth = with(LocalDensity.current) { 48.dp.roundToPx() }
    LaunchedEffect(current, media.size, width, transitions) {
        if (media.isNotEmpty() && width > 0) {
            val offset = -((width - thumbnailWidth) / 2).coerceAtLeast(0)
            if (transitions) state.animateScrollToItem(current.coerceIn(media.indices), offset)
            else state.scrollToItem(current.coerceIn(media.indices), offset)
        }
    }
    LazyRow(state = state, modifier = Modifier.fillMaxWidth().height(70.dp).onSizeChanged { width = it.width },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        itemsIndexed(media, key = { _, item -> item.uri.toString() }) { index, asset ->
            val active = index == current
            Box(Modifier.width(48.dp).fillMaxHeight().clip(RoundedCornerShape(7.dp))
                .border(if (active) 2.dp else 0.dp, if (active) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(7.dp))
                .semantics { selected = active }.clickable { jump(index) }.padding(if (active) 3.dp else 0.dp)) {
                MediaPreview(asset.uri, stringResource(R.string.thumbnail_item, asset.name), Modifier.fillMaxSize(), androidx.compose.ui.layout.ContentScale.Crop, isVideo = asset.isVideo)
                if (asset.isVideo) Icon(Icons.Default.PlayArrow, null, Modifier.align(Alignment.BottomEnd).size(16.dp).background(Color.Black.copy(alpha = .5f)), tint = Color.White)
            }
        }
    }
}
