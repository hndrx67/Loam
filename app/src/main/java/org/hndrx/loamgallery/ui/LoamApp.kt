package org.hndrx.loamgallery.ui

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hndrx.loamgallery.GalleryViewModel
import org.hndrx.loamgallery.model.MediaAccess
import org.hndrx.loamgallery.R
import org.hndrx.loamgallery.model.MediaAsset
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class Tab(val label: Int) {
    Pictures(R.string.pictures), Albums(R.string.albums), Favorites(R.string.favorites), Settings(R.string.settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoamApp(vm: GalleryViewModel, requestAccess: () -> Unit, openSettings: () -> Unit) {
    val library by vm.library.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val preferences by vm.settings.collectAsStateWithLifecycle()
    val external by vm.external.collectAsStateWithLifecycle()
    val externalError by vm.externalError.collectAsStateWithLifecycle()
    val trash by vm.trash.collectAsStateWithLifecycle()
    val busy by vm.operationBusy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val messageText = message?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) { snackbar.showSnackbar(messageText); vm.dismissMessage() }
    }
    var tab by rememberSaveable { mutableStateOf(Tab.Pictures) }
    var selectedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var albumId by rememberSaveable { mutableStateOf<String?>(null) }
    var recycleBin by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val viewerItems by vm.viewerCollection.collectAsStateWithLifecycle()
    val libraryStateHolder = rememberSaveableStateHolder()
    val media = library.media
    val visible = remember(media, favorites, tab, albumId, query) {
        val search = query.trim()
        if (albumId == null && tab != Tab.Favorites && search.isEmpty()) media else media.filter {
            (albumId == null || it.bucketId == albumId) &&
                (tab != Tab.Favorites || it.id.toString() in favorites) &&
                (search.isEmpty() || it.name.contains(search, true) || it.bucketName.contains(search, true))
        }
    }
    val albums = remember(visible) { visible.groupBy { it.bucketId }.values.sortedBy { it.first().bucketName.lowercase() } }
    val byUri = remember(media) { media.associateBy { it.uri.toString() } }
    val session = remember(viewerItems, visible, byUri) {
        viewerItems.ifEmpty { visible }.mapNotNull { byUri[it.uri.toString()] }
    }
    LaunchedEffect(library.loaded, library.loading, session.isEmpty()) {
        if (library.loaded && !library.loading && session.isEmpty()) { selectedUri = null; vm.closeCollection() }
    }
    LaunchedEffect(tab, recycleBin) { if (tab == Tab.Settings) vm.refreshTrash() }
    val viewing = external != null || (selectedUri != null && session.isNotEmpty())
    fun back() {
        when { recycleBin -> recycleBin = false; searching -> { searching = false; query = "" }; albumId != null -> albumId = null }
    }
    BackHandler(enabled = !viewing && (albumId != null || searching || recycleBin)) { back() }
    LoamTheme(preferences) {
        ImageLoading(preferences) {
            val imageLoader = LocalLoamImages.current
            val preload by vm.thumbnailPreload.collectAsStateWithLifecycle()
            Box(Modifier.fillMaxSize()) {
                if (external != null) {
                    Viewer(listOf(external!!), external!!.uri.toString(), emptySet(), {}, null, close = vm::closeExternal)
                } else if (viewing) {
                    Viewer(session, selectedUri!!, favorites, { selectedUri = it.uri.toString() }, vm::toggleFavorite,
                        delete = { asset, action -> vm.requestOperation(asset, action) }, operationBusy = busy) {
                        selectedUri = null
                        vm.closeCollection()
                    }
                } else libraryStateHolder.SaveableStateProvider("library") {
                    val gridStateHolder = rememberSaveableStateHolder()
                    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                    val pageTitle = when {
                        recycleBin -> stringResource(R.string.recycle_bin)
                        albumId != null -> media.firstOrNull { it.bucketId == albumId }?.bucketName ?: stringResource(R.string.album)
                        else -> stringResource(tab.label)
                    }
                    Scaffold(
                        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
                        topBar = {
                            LargeTopAppBar(
                                title = { Column {
                                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                                    Text(pageTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                } },
                                navigationIcon = { if (albumId != null || recycleBin) IconButton(onClick = { back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                                actions = {
                                    if (tab != Tab.Settings) IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { Icon(Icons.Default.Search, stringResource(R.string.search)) }
                                    Box {
                                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }
                                        DropdownMenu(menu, { menu = false }) {
                                            DropdownMenuItem(text = { Text(stringResource(R.string.refresh)) }, onClick = { menu = false; if (recycleBin) vm.refreshTrash() else vm.refresh() }, leadingIcon = { Icon(Icons.Default.Refresh, null) })
                                            DropdownMenuItem(text = { Text(stringResource(R.string.manage_access)) }, onClick = { menu = false; requestAccess() }, leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) })
                                            DropdownMenuItem(text = { Text(stringResource(R.string.settings)) }, onClick = { menu = false; tab = Tab.Settings; albumId = null; searching = false; query = "" }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                                        }
                                    }
                                },
                                expandedHeight = 164.dp,
                                scrollBehavior = scroll,
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background),
                            )
                        },
                        bottomBar = {
                            if (albumId == null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                Tab.entries.forEach { item ->
                                    NavigationBarItem(selected = tab == item, onClick = { tab = item; recycleBin = false; searching = false; query = "" }, icon = {
                                        Icon(when (item) { Tab.Pictures -> Icons.Default.Photo; Tab.Albums -> Icons.Default.Collections; Tab.Favorites -> Icons.Default.FavoriteBorder; Tab.Settings -> Icons.Default.Settings }, null)
                                    }, label = { Text(stringResource(item.label), fontWeight = if (tab == item) FontWeight.Bold else FontWeight.Medium) })
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbar) },
                    ) { padding ->
                        Column(Modifier.padding(padding).fillMaxSize()) {
                            if (searching && tab != Tab.Settings) OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), singleLine = true,
                                placeholder = { Text(stringResource(R.string.search_hint)) }, leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = { IconButton(onClick = { query = ""; searching = false }) { Icon(Icons.Default.Close, stringResource(R.string.clear_search)) } }, shape = RoundedCornerShape(28.dp))
                            if (tab != Tab.Settings && library.access == MediaAccess.Limited) Notice(stringResource(R.string.limited_access), stringResource(R.string.choose_media), requestAccess)
                            if (tab != Tab.Settings && library.failed) Notice(stringResource(R.string.load_failed), stringResource(R.string.retry), vm::refresh)
                            if (library.loading && tab != Tab.Settings) LinearProgressIndicator(Modifier.fillMaxWidth())
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                when {
                                    tab == Tab.Settings && recycleBin -> RecycleBinScreen(trash, busy, vm::refreshTrash) { entry, action -> vm.requestOperation(entry.asset, action, entry.localKey) }
                                    tab == Tab.Settings -> gridStateHolder.SaveableStateProvider("settings") {
                                        SettingsScreen(preferences, library.access, trash.items.size, vm::updateSettings,
                                            { recycleBin = true }, requestAccess, openSettings, { imageLoader.memoryCache?.clear(); vm.cacheCleared() },
                                            preload, library.loaded && !library.loading && library.media.any { !it.isVideo }, vm::startThumbnailPreload)
                                    }
                                    library.access == MediaAccess.None -> EmptyState(R.string.permission_title, R.string.permission_message, Icons.Default.PhotoLibrary) {
                                        Button(onClick = requestAccess) { Text(stringResource(R.string.allow_access)) }
                                        Text(stringResource(R.string.permission_help), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                        TextButton(onClick = openSettings) { Text(stringResource(R.string.open_settings)) }
                                    }
                                    visible.isEmpty() && !library.loading -> EmptyState(
                                        when { library.failed -> R.string.load_failed; query.isNotBlank() -> R.string.no_results; albumId != null -> R.string.empty_album; tab == Tab.Favorites -> R.string.empty_favorites; else -> R.string.empty_library },
                                        when { library.failed -> R.string.load_failed_message; query.isNotBlank() -> R.string.no_results_message; albumId != null -> R.string.empty_album_message; tab == Tab.Favorites -> R.string.empty_favorites_message; else -> R.string.empty_library_message },
                                        if (tab == Tab.Favorites) Icons.Default.FavoriteBorder else Icons.Default.PhotoLibrary,
                                    )
                                    else -> gridStateHolder.SaveableStateProvider("${tab.name}:${albumId.orEmpty()}") {
                                        if (tab == Tab.Albums && albumId == null) AlbumGrid(albums) { albumId = it }
                                        else MediaGrid(visible, preferences.columns, favorites) { vm.openCollection(visible); selectedUri = it.uri.toString() }
                                    }
                                }
                            }
                        }
                    }
                }
                if (viewing) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 150.dp))
                if (externalError) AlertDialog(onDismissRequest = vm::dismissExternalError,
                    confirmButton = { TextButton(onClick = vm::dismissExternalError) { Text(stringResource(R.string.close)) } },
                    text = { Text(stringResource(R.string.external_failed)) })
            }
        }
    }
}
@Composable
private fun Notice(text: String, action: String, onAction: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun MediaGrid(media: List<MediaAsset>, columns: Int, favorites: Set<String>, open: (MediaAsset) -> Unit) {
    val zone = ZoneId.systemDefault()
    val sections = remember(media, zone) { media.groupBy { Instant.ofEpochSecond(it.dateAdded).atZone(zone).toLocalDate() } }
    LazyVerticalGrid(GridCells.Fixed(columns), contentPadding = PaddingValues(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(pluralStringResource(R.plurals.item_count, media.size, media.size), Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        sections.forEach { (date, items) ->
            item(key = "date:$date", span = { GridItemSpan(maxLineSpan) }) {
                Text(dateLabel(date), Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            }
            items(items, key = { it.uri.toString() }, contentType = { "media" }) { item ->
                Box(Modifier.aspectRatio(1f).clickable { open(item) }) {
                    MediaPreview(item.uri, item.name, Modifier.fillMaxSize(), ContentScale.Crop)
                    if (item.id.toString() in favorites) Icon(Icons.Default.Favorite, null, Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp), tint = Color.White)
                    if (item.isVideo) Surface(Modifier.align(Alignment.BottomEnd).padding(5.dp), color = Color.Black.copy(alpha = .6f), shape = RoundedCornerShape(6.dp)) {
                        Row(Modifier.padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(14.dp), tint = Color.White)
                            Text(formatDuration(item.duration), color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<List<MediaAsset>>, open: (String) -> Unit) {
    LazyVerticalGrid(GridCells.Adaptive(150.dp), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { Text(pluralStringResource(R.plurals.album_count, albums.size, albums.size), Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(albums, key = { it.first().bucketId }, contentType = { "album" }) { items ->
            val cover = items.first()
            Column(Modifier.clip(RoundedCornerShape(20.dp)).clickable { open(cover.bucketId) }) {
                MediaPreview(cover.uri, cover.bucketName, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)), ContentScale.Crop)
                Text(cover.bucketName, Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.item_count, items.size, items.size), Modifier.padding(start = 4.dp, top = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyState(title: Int, message: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, actions: @Composable ColumnScope.() -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Icon(icon, null, Modifier.padding(24.dp).size(42.dp), tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        actions()
    }
}

@Composable
private fun dateLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> stringResource(R.string.today)
    LocalDate.now().minusDays(1) -> stringResource(R.string.yesterday)
    else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}

fun formatDuration(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
}

fun shareIntent(item: MediaAsset) = Intent(Intent.ACTION_SEND).apply {
    type = item.mimeType
    putExtra(Intent.EXTRA_STREAM, item.uri)
    clipData = ClipData.newRawUri(item.name, item.uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

