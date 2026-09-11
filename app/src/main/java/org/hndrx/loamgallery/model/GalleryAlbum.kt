package org.hndrx.loamgallery.model

/** A device bucket or a persistent album organized within Loam. */
data class GalleryAlbum(val id: String, val name: String, val media: List<MediaAsset>, val custom: Boolean = false)
data class SavedAlbum(val id: String, val name: String, val uris: Set<String>)

fun sortedAlbums(albums: List<GalleryAlbum>, sort: AlbumSort): List<GalleryAlbum> = when (sort) {
    AlbumSort.NameAscending -> albums.sortedBy { it.name.lowercase() }
    AlbumSort.NameDescending -> albums.sortedByDescending { it.name.lowercase() }
    AlbumSort.Newest -> albums.sortedByDescending { it.media.maxOfOrNull { asset -> asset.dateAdded } ?: 0 }
    AlbumSort.MostItems -> albums.sortedByDescending { it.media.size }
}
