package org.hndrx.loamgallery.model

import org.hndrx.loamgallery.ui.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DurationTest {
    @Test fun durationsHandleInvalidMetadataAndHourBoundaries() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("0:00", formatDuration(-1000))
            assertEquals("0:59", formatDuration(59_999))
            assertEquals("1:00", formatDuration(60_000))
            assertEquals("59:59", formatDuration(3_599_000))
            assertEquals("1:00:00", formatDuration(3_600_000))
            assertEquals("2:01:05", formatDuration(7_265_000))
        } finally { Locale.setDefault(previous) }
    }
}
