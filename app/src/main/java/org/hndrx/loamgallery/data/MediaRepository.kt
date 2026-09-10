package org.hndrx.loamgallery.data

import android.content.ContentUris
import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.hndrx.loamgallery.R
import org.hndrx.loamgallery.model.MediaAsset

class MediaRepository(private val context: Context) {
    suspend fun load(trashed: Boolean = false): List<MediaAsset> = withContext(Dispatchers.IO) {
        if (trashed && Build.VERSION.SDK_INT < 30) return@withContext emptyList()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = mutableListOf(
            MediaStore.Files.FileColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.BUCKET_ID, MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Video.VideoColumns.DURATION, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT,
        )
        if (Build.VERSION.SDK_INT >= 29) projection += MediaStore.MediaColumns.VOLUME_NAME
        if (Build.VERSION.SDK_INT >= 30) projection += MediaStore.MediaColumns.DATE_EXPIRES
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val args = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        val result = mutableListOf<MediaAsset>()
        val sort = "${MediaStore.MediaColumns.DATE_ADDED} DESC, ${MediaStore.MediaColumns._ID} DESC"
        val cursor = (if (Build.VERSION.SDK_INT >= 30) {
            context.contentResolver.query(collection, projection.toTypedArray(), Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sort)
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, if (trashed) MediaStore.MATCH_ONLY else MediaStore.MATCH_EXCLUDE)
            }, null)
        } else context.contentResolver.query(collection, projection.toTypedArray(), selection, args, sort))
            ?: error("Media provider did not return a cursor")
        cursor.use { c ->
            val indices = projection.map(c::getColumnIndexOrThrow)
            while (c.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val id = c.getLong(indices[0])
                val mime = c.getString(indices[2]) ?: "image/*"
                val volume = if (Build.VERSION.SDK_INT >= 29) c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME)) ?: "external" else "external"
                val itemCollection = if (mime.startsWith("video/")) MediaStore.Video.Media.getContentUri(volume) else MediaStore.Images.Media.getContentUri(volume)
                result += MediaAsset(
                    id, ContentUris.withAppendedId(itemCollection, id),
                    c.getString(indices[1]) ?: context.getString(R.string.untitled),
                    mime, c.getLong(indices[3]),
                    c.getString(indices[4]) ?: "other",
                    c.getString(indices[5]) ?: context.getString(R.string.other_album),
                    c.getLong(indices[6]), c.getLong(indices[7]),
                    c.getInt(indices[8]), c.getInt(indices[9]),
                    if (Build.VERSION.SDK_INT >= 30) c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES)) else 0,
                )
            }
        }
        result
    }

    suspend fun resolve(uri: Uri): MediaAsset = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val extension = uri.lastPathSegment?.substringAfterLast('.')?.lowercase().orEmpty()
        val type = resolver.getType(uri) ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/*"
        require(type.startsWith("image/") || type.startsWith("video/"))
        // Verify the temporary grant before showing a viewer. Never require library access here.
        resolver.openAssetFileDescriptor(uri, "r")?.use { } ?: error("Media is unavailable")
        var name = uri.lastPathSegment ?: context.getString(R.string.untitled)
        var size = 0L
        if (uri.scheme == "content") resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
            }
        }
        MediaAsset(-1, uri, name, type, 0, "external", context.getString(R.string.external_media), 0, size, 0, 0)
    }
}
