package org.hndrx.loamgallery.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.hndrx.loamgallery.BuildConfig
import org.hndrx.loamgallery.R
import org.hndrx.loamgallery.model.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: AppSettings, access: MediaAccess, trashCount: Int, update: (AppSettings) -> Unit,
    openBin: () -> Unit, requestAccess: () -> Unit, openSystemSettings: () -> Unit, clearCache: () -> Unit,
    preload: org.hndrx.loamgallery.ThumbnailPreloadState, canPreload: Boolean, startPreload: () -> Unit) {
    var columns by remember(settings.columns) { mutableFloatStateOf(settings.columns.toFloat()) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            SettingsSection(R.string.appearance_section) {
                Text(stringResource(R.string.theme_mode), fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(selected = settings.theme == mode, onClick = { update(settings.copy(theme = mode)) }, label = { Text(stringResource(when (mode) { ThemeMode.System -> R.string.theme_system; ThemeMode.Light -> R.string.theme_light; ThemeMode.Dark -> R.string.theme_dark })) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.color_scheme), fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.entries.forEach { palette ->
                        FilterChip(selected = settings.palette == palette, onClick = { update(settings.copy(palette = palette)) },
                            leadingIcon = { Surface(Modifier.size(18.dp), shape = CircleShape, color = paletteColor(palette)) {} },
                            label = { Text(stringResource(when (palette) { Palette.Blue -> R.string.palette_blue; Palette.Sage -> R.string.palette_sage; Palette.Violet -> R.string.palette_violet; Palette.Rose -> R.string.palette_rose; Palette.Amber -> R.string.palette_amber })) })
                    }
                }
            }
        }
        item {
            SettingsSection(R.string.viewer_section) {
                Text(stringResource(R.string.grid_columns, columns.toInt()), fontWeight = FontWeight.Medium)
                Slider(columns, { columns = it }, onValueChangeFinished = { update(settings.copy(columns = columns.toInt())) }, valueRange = 2f..6f, steps = 3)
                SettingSwitch(R.string.filmstrip, R.string.filmstrip_description, settings.filmstrip) { update(settings.copy(filmstrip = it)) }
            }
        }
        item {
            SettingsSection(R.string.performance_section, experimental = true) {
                HelpText(R.string.performance_description)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.thumbnail_quality), fontWeight = FontWeight.Medium)
                HelpText(R.string.thumbnail_quality_description)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(192 to R.string.quality_fast, 384 to R.string.quality_balanced, 768 to R.string.quality_detailed).forEach { (size, label) ->
                        FilterChip(settings.thumbnailSize == size, { update(settings.copy(thumbnailSize = size)) }, label = { Text(stringResource(label)) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.image_cache), fontWeight = FontWeight.Medium)
                HelpText(R.string.cache_description)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(32, 64, 128).forEach { size -> FilterChip(settings.memoryCacheMb == size, { update(settings.copy(memoryCacheMb = size)) }, label = { Text(stringResource(R.string.cache_size, size)) }) }
                }
                SettingSwitch(R.string.preload_images, R.string.preload_description, settings.prefetch) { update(settings.copy(prefetch = it)) }
                SettingSwitch(R.string.low_memory_thumbnails, R.string.low_memory_description, settings.lowMemoryThumbnails) { update(settings.copy(lowMemoryThumbnails = it)) }
                SettingSwitch(R.string.video_thumbnails, R.string.video_thumbnails_description, settings.videoThumbnails) { update(settings.copy(videoThumbnails = it)) }
                SettingSwitch(R.string.image_transitions, R.string.image_transitions_description, settings.transitions) { update(settings.copy(transitions = it)) }
                SettingSwitch(R.string.watch_changes, R.string.watch_changes_description, settings.watchChanges) { update(settings.copy(watchChanges = it)) }
                HelpText(R.string.thumbnail_preload_description)
                Button(onClick = startPreload, enabled = canPreload && !preload.running) {
                    Text(stringResource(R.string.start_thumbnail_preload))
                }
                if (preload.total > 0) {
                    if (preload.running) LinearProgressIndicator(
                        progress = { preload.completed.toFloat() / preload.total }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(if (preload.running) R.string.thumbnail_preload_progress else R.string.thumbnail_preload_complete,
                        preload.completed - preload.failed, preload.total, preload.failed), style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = clearCache, enabled = !preload.running) { Text(stringResource(R.string.clear_cache)) }
                TextButton(onClick = { update(settings.resetPerformance()) }) { Text(stringResource(R.string.reset_performance)) }
            }
        }
        item {
            SettingsSection(R.string.storage_section) {
                Surface(onClick = openBin, color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteOutline, null)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(stringResource(R.string.recycle_bin), fontWeight = FontWeight.SemiBold)
                            Text(pluralStringResource(R.plurals.item_count, trashCount, trashCount), style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                }
                Spacer(Modifier.height(12.dp))
                HelpText(R.string.recycle_description)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(when (access) { MediaAccess.None -> R.string.access_none; MediaAccess.Limited -> R.string.access_limited; MediaAccess.Full -> R.string.access_full }), fontWeight = FontWeight.Medium)
                TextButton(onClick = requestAccess) { Text(stringResource(R.string.manage_access)) }
                TextButton(onClick = openSystemSettings) { Text(stringResource(R.string.open_settings)) }
            }
        }
        item {
            SettingsSection(R.string.about_section) {
                Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                HelpText(R.string.about_description)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: Int, experimental: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, false))
            if (experimental) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                Text(stringResource(R.string.experimental), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(18.dp), content = content)
        }
    }
}

@Composable
private fun SettingSwitch(title: Int, description: Int, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = change).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(title), fontWeight = FontWeight.Medium)
            HelpText(description)
        }
        Switch(checked, onCheckedChange = null)
    }
}

@Composable
private fun HelpText(text: Int) { Text(stringResource(text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
