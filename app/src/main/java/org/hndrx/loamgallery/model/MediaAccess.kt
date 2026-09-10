package org.hndrx.loamgallery.model

enum class MediaAccess { None, Limited, Full }

fun resolveMediaAccess(api: Int, images: Boolean, videos: Boolean, selected: Boolean, storage: Boolean): MediaAccess = when {
    api < 33 -> if (storage) MediaAccess.Full else MediaAccess.None
    images && videos -> MediaAccess.Full
    images || videos || (api >= 34 && selected) -> MediaAccess.Limited
    else -> MediaAccess.None
}
