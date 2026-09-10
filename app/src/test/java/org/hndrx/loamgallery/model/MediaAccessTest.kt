package org.hndrx.loamgallery.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaAccessTest {
    @Test fun legacyStorageGrantProvidesFullAccess() {
        assertEquals(MediaAccess.Full, resolveMediaAccess(32, false, false, false, true))
    }

    @Test fun legacyDenialDoesNotUseModernGrants() {
        assertEquals(MediaAccess.None, resolveMediaAccess(32, true, true, true, false))
    }

    @Test fun android13RequiresBothCategoriesForFullAccess() {
        assertEquals(MediaAccess.Limited, resolveMediaAccess(33, true, false, false, false))
        assertEquals(MediaAccess.Limited, resolveMediaAccess(33, false, true, false, false))
        assertEquals(MediaAccess.Full, resolveMediaAccess(33, true, true, false, false))
    }

    @Test fun selectedAccessIsRecognizedOnlyOnAndroid14AndLater() {
        assertEquals(MediaAccess.None, resolveMediaAccess(33, false, false, true, false))
        assertEquals(MediaAccess.Limited, resolveMediaAccess(34, false, false, true, false))
        assertEquals(MediaAccess.Limited, resolveMediaAccess(36, false, false, true, false))
    }

    @Test fun oldStorageGrantDoesNotAuthorizeModernLibrary() {
        assertEquals(MediaAccess.None, resolveMediaAccess(34, false, false, false, true))
    }

    @Test fun fullAccessTakesPrecedenceOverSelectedGrant() {
        assertEquals(MediaAccess.Full, resolveMediaAccess(34, true, true, true, false))
    }
}
