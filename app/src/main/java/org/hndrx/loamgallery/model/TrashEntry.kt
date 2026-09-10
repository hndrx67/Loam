package org.hndrx.loamgallery.model

data class TrashEntry(val asset: MediaAsset, val localKey: String? = null, val expiresAt: Long = 0)
enum class MediaAction { Trash, Delete, Restore }
