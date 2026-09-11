package org.hndrx.loamgallery.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Scale
import coil3.toBitmap
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hndrx.loamgallery.model.AppSettings
import java.io.File
import java.security.MessageDigest

/** Only explicit preload runs write here; normal gallery requests only read. */
object ThumbnailCache {
    private fun directory(context: Context) = File(context.cacheDir, "preloaded_thumbnails")

    fun file(context: Context, uri: Uri, settings: AppSettings): File {
        val key = "$uri:${settings.thumbnailSize}"
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory(context), "$hash.png")
    }

    suspend fun preload(context: Context, loader: ImageLoader, uri: Uri, settings: AppSettings) = withContext(Dispatchers.IO) {
        val target = file(context, uri, settings)
        if (target.isFile && target.length() > 0L) return@withContext
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        val result = loader.execute(ImageRequest.Builder(context).data(uri)
            .size(settings.thumbnailSize).scale(Scale.FILL).allowHardware(false).build())
        check(result is SuccessResult) { "Thumbnail decoding failed" }
        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
        val temporary = File.createTempFile("thumbnail", ".tmp", target.parentFile)
        try {
            temporary.outputStream().use { check(result.image.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)) }
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            check(temporary.renameTo(target)) { "Thumbnail cache write failed" }
        } finally {
            temporary.delete()
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        directory(context).listFiles()?.forEach { check(it.delete()) { "Thumbnail cache clear failed" } }
    }
}
