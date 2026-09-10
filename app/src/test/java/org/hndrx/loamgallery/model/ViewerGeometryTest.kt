package org.hndrx.loamgallery.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerGeometryTest {
    @Test fun fittedImageCannotBeDraggedAtOriginalScale() {
        assertEquals(0f, panLimit(1080f, 1080f, 1f), .01f)
    }

    @Test fun letterboxedAxisCannotPanUntilItExceedsViewport() {
        assertEquals(0f, panLimit(1920f, 600f, 2f), .01f)
        assertEquals(240f, panLimit(1920f, 600f, 4f), .01f)
    }

    @Test fun zoomedImageCanPanExactlyToItsEdge() {
        assertEquals(540f, panLimit(1080f, 1080f, 2f), .01f)
    }

    @Test fun emptyViewportHasNoPanAtZeroContentSize() {
        assertEquals(0f, panLimit(0f, 0f, 5f), .01f)
    }
}
