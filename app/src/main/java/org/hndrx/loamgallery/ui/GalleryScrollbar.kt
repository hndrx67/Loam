package org.hndrx.loamgallery.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BoxScope.GalleryScrollbar(state: LazyGridState, enabled: Boolean) {
    var visible by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(state.isScrollInProgress, dragging, enabled) {
        if (enabled && (state.isScrollInProgress || dragging)) visible = true
        else { delay(1000); visible = false }
    }
    if (!enabled || !visible || (!state.canScrollForward && !state.canScrollBackward)) return
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(28.dp)
        .pointerInput(state) {
            fun seek(y: Float) {
                val target = ((y / size.height).coerceIn(0f, 1f) * (state.layoutInfo.totalItemsCount - 1)).toInt().coerceAtLeast(0)
                scrollJob?.cancel()
                scrollJob = scope.launch { state.scrollToItem(target) }
            }
            detectVerticalDragGestures(onDragStart = { dragging = true; seek(it.y) },
                onDragEnd = { dragging = false }, onDragCancel = { dragging = false }) { change, _ ->
                change.consume(); seek(change.position.y)
            }
        }) {
        val info = state.layoutInfo
        val count = info.totalItemsCount.coerceAtLeast(1)
        val fraction = info.visibleItemsInfo.size.toFloat() / count
        val height = (size.height * fraction).coerceIn(minOf(36.dp.toPx(), size.height), size.height)
        val maxIndex = (count - info.visibleItemsInfo.size).coerceAtLeast(1)
        val progress = if (!state.canScrollForward) 1f else (state.firstVisibleItemIndex.toFloat() / maxIndex).coerceIn(0f, 1f)
        drawRoundRect(color.copy(alpha = .85f), Offset(size.width - 9.dp.toPx(), (size.height - height) * progress),
            Size(5.dp.toPx(), height), CornerRadius(3.dp.toPx()))
    }
}
