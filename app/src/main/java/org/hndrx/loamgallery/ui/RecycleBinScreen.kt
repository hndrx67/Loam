package org.hndrx.loamgallery.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.hndrx.loamgallery.R
import org.hndrx.loamgallery.TrashState
import org.hndrx.loamgallery.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RecycleBinScreen(state: TrashState, busy: Boolean, refresh: () -> Unit, operation: (TrashEntry, MediaAction) -> Unit) {
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = state.items.firstOrNull { (it.localKey ?: it.asset.uri.toString()) == deleting }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(stringResource(if (Build.VERSION.SDK_INT >= 30) R.string.native_bin_description else R.string.legacy_bin_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (Build.VERSION.SDK_INT >= 30 && state.items.any { it.localKey != null }) Text(stringResource(R.string.legacy_bin_description), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall)
            if (state.loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
        }
        if (state.failed) item {
            Text(stringResource(R.string.bin_failed), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = refresh) { Text(stringResource(R.string.retry)) }
        }
        if (!state.loading && state.items.isEmpty() && !state.failed) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.empty_bin), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.empty_bin_description), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        items(state.items, key = { it.localKey ?: it.asset.uri.toString() }) { entry ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MediaPreview(entry.asset.uri, entry.asset.name, Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)), ContentScale.Crop, isVideo = entry.asset.isVideo)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(entry.asset.name, fontWeight = FontWeight.Medium)
                            if (entry.localKey != null) Text(stringResource(R.string.local_recovery_copy), style = MaterialTheme.typography.bodySmall)
                            else if (entry.expiresAt > 0) Text(stringResource(R.string.expires_on, Instant.ofEpochSecond(entry.expiresAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { operation(entry, MediaAction.Restore) }, enabled = !busy) { Text(stringResource(R.string.restore)) }
                        TextButton(onClick = { deleting = entry.localKey ?: entry.asset.uri.toString() }, enabled = !busy) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        if (state.items.any { it.localKey == null }) item { Text(stringResource(R.string.preview_unavailable_bin), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (selected != null) DeletionDialog(selected.asset, fromBin = true, localCopy = selected.localKey != null,
        dismiss = { deleting = null }, confirm = { action -> deleting = null; operation(selected, action) })
}

@Composable
fun DeletionDialog(asset: MediaAsset, fromBin: Boolean = false, localCopy: Boolean = false, dismiss: () -> Unit, confirm: (MediaAction) -> Unit) {
    var permanent by rememberSaveable(asset.uri.toString()) { mutableStateOf(fromBin) }
    AlertDialog(onDismissRequest = dismiss,
        title = { Text(stringResource(if (permanent) R.string.delete_permanently else R.string.delete_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(asset.name, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                if (!fromBin) Column(Modifier.selectableGroup()) {
                    listOf(false to R.string.move_to_bin, true to R.string.delete_permanently).forEach { (value, label) ->
                        Row(Modifier.fillMaxWidth().selectable(permanent == value, role = Role.RadioButton, onClick = { permanent = value }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(permanent == value, onClick = null)
                            Text(stringResource(label), Modifier.padding(start = 10.dp))
                        }
                    }
                }
                Text(stringResource(if (permanent) { if (localCopy) R.string.local_permanent_warning else R.string.permanent_warning }
                    else if (Build.VERSION.SDK_INT >= 30) R.string.trash_native_hint else R.string.trash_legacy_hint), Modifier.padding(top = 12.dp), color = if (permanent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { confirm(if (permanent) MediaAction.Delete else MediaAction.Trash) }) { Text(stringResource(if (permanent) R.string.delete_permanently else R.string.move_to_bin), color = if (permanent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
