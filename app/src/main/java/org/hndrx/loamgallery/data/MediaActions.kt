package org.hndrx.loamgallery.data

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hndrx.loamgallery.model.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaActions(private val context: Context, private val repository: MediaRepository) {
    private val resolver = context.contentResolver
    private val archive = TrashArchive(File(context.noBackupFilesDir, "recycle_bin"))

    suspend fun trash(includeNative: Boolean = true): List<TrashEntry> = withContext(Dispatchers.IO) {
        val native = if (includeNative && Build.VERSION.SDK_INT >= 30) repository.load(trashed = true).map { TrashEntry(it, expiresAt = it.expiresAt) } else emptyList()
        native + archive.keys().mapNotNull { key ->
            val metadata = archive.read(key)
            // A crash between deleting the original and updating its journal is recoverable.
            if (metadata["state"] == "prepared" && sourceExists(metadata.getValue("uri").toUri()) != false) null
            else TrashEntry(assetFromMetadata(metadata).copy(uri = Uri.fromFile(archive.payload(key))), key)
        }
    }

    /** Returns the system consent request; Android 11+ performs the action when approved. */
    suspend fun execute(asset: MediaAsset, action: MediaAction, localKey: String?): IntentSender? = withContext(Dispatchers.IO) {
        if (localKey != null && action != MediaAction.Trash) {
            when (action) {
                MediaAction.Restore -> restore(localKey)
                MediaAction.Delete -> archive.discard(localKey)
                else -> error("Unsupported action")
            }
        } else if (Build.VERSION.SDK_INT >= 30) {
            require(asset.uri.authority == MediaStore.AUTHORITY)
            return@withContext when (action) {
                MediaAction.Trash -> MediaStore.createTrashRequest(resolver, listOf(asset.uri), true)
                MediaAction.Restore -> MediaStore.createTrashRequest(resolver, listOf(asset.uri), false)
                MediaAction.Delete -> MediaStore.createDeleteRequest(resolver, listOf(asset.uri))
            }.intentSender
        } else {
            when (action) {
                MediaAction.Trash -> {
                    val key = requireNotNull(localKey)
                    archive.stage(key, assetMetadata(asset)) {
                        val original = if (Build.VERSION.SDK_INT == 29) MediaStore.setRequireOriginal(asset.uri) else asset.uri
                        resolver.openInputStream(original) ?: error("Media is unavailable")
                    }
                    // The verified recovery copy and its journal are durable before deletion.
                    check(resolver.delete(asset.uri, null, null) > 0) { "Original could not be removed" }
                    archive.write(key, archive.read(key) + ("state" to "trashed"))
                }
                MediaAction.Delete -> check(resolver.delete(asset.uri, null, null) > 0) { "Media could not be removed" }
                MediaAction.Restore -> error("Missing recovery copy")
            }
        }
        null
    }

    suspend fun cancelPrepared(key: String?, source: Uri) = withContext(Dispatchers.IO) {
        if (key != null && sourceExists(source) == true && key in archive.keys() && archive.read(key)["state"] == "prepared") archive.discard(key)
    }

    private fun sourceExists(uri: Uri): Boolean? = try {
        resolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { it.moveToFirst() }
    } catch (_: Exception) { null }

    @Suppress("DEPRECATION")
    private fun restore(key: String) {
        archive.verify(key)
        var metadata = archive.read(key)
        val asset = assetFromMetadata(metadata)
        val volume = if (Build.VERSION.SDK_INT >= 29) MediaStore.VOLUME_EXTERNAL_PRIMARY else "external"
        val collection = if (asset.isVideo) MediaStore.Video.Media.getContentUri(volume) else MediaStore.Images.Media.getContentUri(volume)
        val folder = if (asset.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        val existing = metadata["restoreUri"]?.toUri()
        val destination = existing ?: run {
            val safeName = asset.name.replace('/', '_').replace('\\', '_')
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                put(MediaStore.MediaColumns.MIME_TYPE, asset.mimeType)
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/Loam/")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                } else {
                    val directory = File(Environment.getExternalStoragePublicDirectory(folder), "Loam")
                    check(directory.isDirectory || directory.mkdirs())
                    put(MediaStore.MediaColumns.DATA, File(directory, "${UUID.randomUUID()}_$safeName").absolutePath)
                }
            }
            val inserted = resolver.insert(collection, values) ?: error("Could not create restored media")
            try {
                metadata = metadata + mapOf("restoreUri" to inserted.toString(), "state" to "restoring")
                archive.write(key, metadata)
            } catch (error: Exception) {
                resolver.delete(inserted, null, null)
                throw error
            }
            inserted
        }
        if (metadata["state"] !in listOf("copied", "restored")) {
            val descriptor = resolver.openFileDescriptor(destination, "wt") ?: error("Could not write restored media")
                ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { output ->
                    val count = archive.payload(key).inputStream().use { it.copyTo(output) }
                    check(count == archive.payload(key).length())
                    output.fd.sync()
                }
            metadata = metadata + ("state" to "copied")
            archive.write(key, metadata)
        }
        if (Build.VERSION.SDK_INT >= 29) check(resolver.update(destination, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) > 0)
        archive.write(key, metadata + ("state" to "restored"))
        archive.discard(key)
    }
}

fun assetMetadata(asset: MediaAsset): Map<String, String> = mapOf(
    "id" to asset.id.toString(), "uri" to asset.uri.toString(), "name" to asset.name,
    "mime" to asset.mimeType, "date" to asset.dateAdded.toString(), "bucketId" to asset.bucketId,
    "bucketName" to asset.bucketName, "duration" to asset.duration.toString(), "size" to asset.size.toString(),
    "width" to asset.width.toString(), "height" to asset.height.toString(),
)

fun assetFromMetadata(data: Map<String, String>) = MediaAsset(
    data.getValue("id").toLong(), data.getValue("uri").toUri(), data.getValue("name"), data.getValue("mime"),
    data.getValue("date").toLong(), data.getValue("bucketId"), data.getValue("bucketName"),
    data.getValue("duration").toLong(), data.getValue("size").toLong(), data.getValue("width").toInt(), data.getValue("height").toInt(),
)
