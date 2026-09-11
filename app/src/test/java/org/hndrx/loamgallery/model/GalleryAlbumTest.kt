package org.hndrx.loamgallery.model

import org.junit.Assert.*
import org.junit.Test

class GalleryAlbumTest {
    @Test fun sortingIncludesEmptyCustomAlbumsAndIgnoresNameCase() {
        val albums = listOf(GalleryAlbum("z", "Zoo", emptyList()),
            GalleryAlbum("a", "apple", emptyList(), custom = true), GalleryAlbum("b", "Beach", emptyList()))
        assertEquals(listOf("a", "b", "z"), sortedAlbums(albums, AlbumSort.NameAscending).map { it.id })
        assertEquals(listOf("z", "b", "a"), sortedAlbums(albums, AlbumSort.NameDescending).map { it.id })
        assertEquals(3, sortedAlbums(albums, AlbumSort.Newest).size)
        assertEquals(3, sortedAlbums(albums, AlbumSort.MostItems).size)
    }

    @Test fun performanceResetPreservesFeatureAndAlbumPreferences() {
        val settings = AppSettings(scrollbar = false, albumSort = AlbumSort.MostItems, thumbnailSize = 768)
        val reset = settings.resetPerformance()
        assertFalse(reset.scrollbar)
        assertEquals(AlbumSort.MostItems, reset.albumSort)
        assertEquals(384, reset.thumbnailSize)
    }
}
