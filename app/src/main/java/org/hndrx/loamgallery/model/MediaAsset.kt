package org.hndrx.loamgallery.model

import android.net.Uri

data class MediaAsset(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val dateAdded: Long,
    val bucketId: String,
    val bucketName: String,
    val duration: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val expiresAt: Long = 0,
) { val isVideo get() = mimeType.startsWith("video/") }

data class Album(val id: String, val name: String, val cover: Uri, val count: Int)
