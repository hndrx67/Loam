package org.hndrx.loamgallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.hndrx.loamgallery.R
import org.hndrx.loamgallery.model.*

@Composable
fun SelectionBar(count: Int, busy: Boolean, clear: () -> Unit, selectAll: () -> Unit, delete: () -> Unit, add: () -> Unit, canAdd: Boolean) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = clear) { Icon(Icons.Default.Close, stringResource(R.string.clear_selection)) }
            Text(stringResource(R.string.selected_count, count), Modifier.weight(1f))
            IconButton(onClick = selectAll, enabled = !busy) { Icon(Icons.Default.SelectAll, stringResource(R.string.select_all)) }
            IconButton(onClick = add, enabled = !busy && canAdd) { Icon(Icons.Default.CreateNewFolder, stringResource(R.string.add_to_album)) }
            IconButton(onClick = delete, enabled = !busy) { Icon(Icons.Default.DeleteOutline, stringResource(R.string.delete_selection)) }
        }
    }
}

@Composable
fun AlbumControls(sort: AlbumSort, update: (AlbumSort) -> Unit, create: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = create) { Text(stringResource(R.string.create_album)) }
        Box {
            TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.sort_albums)) }
            DropdownMenu(expanded, { expanded = false }) {
                AlbumSort.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(stringResource(when (option) {
                        AlbumSort.NameAscending -> R.string.sort_name_ascending
                        AlbumSort.NameDescending -> R.string.sort_name_descending
                        AlbumSort.Newest -> R.string.sort_newest
                        AlbumSort.MostItems -> R.string.sort_most_items
                    })) }, onClick = { update(option); expanded = false },
                        trailingIcon = { if (sort == option) Icon(Icons.Default.Check, null) })
                }
            }
        }
    }
}

@Composable
fun CreateAlbumDialog(onDismiss: () -> Unit, create: (String) -> Boolean) {
    var name by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.create_album)) },
        text = { Column {
            Text(stringResource(R.string.loam_album_description))
            OutlinedTextField(name, { name = it; error = false }, label = { Text(stringResource(R.string.album_name)) }, singleLine = true, isError = error)
            if (error) Text(stringResource(R.string.album_name_error), color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(onClick = { error = !create(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.create_album)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun AlbumPicker(albums: List<SavedAlbum>, dismiss: () -> Unit, create: () -> Unit, pick: (String) -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.add_to_album)) },
        text = { Column {
            Text(stringResource(R.string.loam_album_description))
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(albums, key = { it.id }) { album ->
                    Text(album.name, Modifier.fillMaxWidth().clickable { pick(album.id) }.padding(vertical = 16.dp))
                }
            }
        } },
        confirmButton = { TextButton(onClick = create) { Text(stringResource(R.string.create_album)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } })
}
