package org.hndrx.loamgallery.model

/** Maximum translation of a fitted image, keeping its edges inside the viewport. */
fun panLimit(viewport: Float, content: Float, scale: Float): Float =
    ((content * scale - viewport) / 2f).coerceAtLeast(0f)
