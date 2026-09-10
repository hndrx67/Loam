package org.hndrx.loamgallery.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.io.InputStream

class TrashArchiveTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun stagedCopyAndJournalSurviveReopeningTheArchive() {
        val root = temporary.newFolder()
        val bytes = ByteArray(100_000) { (it % 251).toByte() }
        TrashArchive(root).stage("entry-1", mapOf("size" to bytes.size.toString(), "name" to "original.jpg")) { bytes.inputStream() }
        val reopened = TrashArchive(root)
        assertEquals(listOf("entry-1"), reopened.keys())
        assertArrayEquals(bytes, reopened.payload("entry-1").readBytes())
        assertEquals("prepared", reopened.read("entry-1")["state"])
        assertEquals("original.jpg", reopened.read("entry-1")["name"])
    }

    @Test fun incompleteCopyIsNeverExposedAsRecoverableMedia() {
        val archive = TrashArchive(temporary.newFolder())
        try {
            archive.stage("short", mapOf("size" to "100")) { byteArrayOf(1, 2, 3).inputStream() }
            fail("A truncated source must fail before deletion is permitted")
        } catch (_: IllegalStateException) { }
        assertTrue(archive.keys().isEmpty())
        assertFalse(archive.payload("short").exists())
    }

    @Test fun readFailureLeavesNoCommittedCopy() {
        val root = temporary.newFolder()
        val archive = TrashArchive(root)
        try {
            archive.stage("failed", emptyMap()) { object : InputStream() {
                override fun read(): Int = throw IOException("Device disconnected")
            } }
            fail("A source failure must be reported")
        } catch (_: IOException) { }
        assertTrue(archive.keys().isEmpty())
        assertTrue(root.listFiles().orEmpty().isEmpty())
    }

    @Test fun retryDoesNotOverwriteAnExistingRecoveryCopy() {
        val archive = TrashArchive(temporary.newFolder())
        archive.stage("retry", mapOf("size" to "3")) { byteArrayOf(1, 2, 3).inputStream() }
        archive.stage("retry", mapOf("size" to "1")) { throw AssertionError("Must reuse the durable copy") }
        assertArrayEquals(byteArrayOf(1, 2, 3), archive.payload("retry").readBytes())
    }

    @Test fun corruptedCopyBlocksDeletionRetryAndRestore() {
        val archive = TrashArchive(temporary.newFolder())
        archive.stage("corrupt", mapOf("size" to "3")) { byteArrayOf(1, 2, 3).inputStream() }
        archive.payload("corrupt").writeBytes(byteArrayOf(9, 9, 9))
        try { archive.verify("corrupt"); fail("A modified recovery copy must not be restored") }
        catch (_: IllegalStateException) { }
        try { archive.stage("corrupt", emptyMap()) { byteArrayOf(1).inputStream() }; fail("Retry must not authorize deletion with an invalid backup") }
        catch (_: IllegalStateException) { }
    }

    @Test fun journalCanRecordDeletionAndRestorePhasesWithoutChangingPayload() {
        val archive = TrashArchive(temporary.newFolder())
        archive.stage("phases", emptyMap()) { byteArrayOf(4, 5, 6).inputStream() }
        archive.write("phases", archive.read("phases") + ("state" to "trashed"))
        archive.write("phases", archive.read("phases") + mapOf("state" to "copied", "restoreUri" to "content://media/external/images/media/42"))
        assertEquals("copied", archive.read("phases")["state"])
        assertEquals("content://media/external/images/media/42", archive.read("phases")["restoreUri"])
        assertArrayEquals(byteArrayOf(4, 5, 6), archive.payload("phases").readBytes())
    }

    @Test fun discardRemovesOnlyTheRequestedEntry() {
        val archive = TrashArchive(temporary.newFolder())
        archive.stage("keep", emptyMap()) { byteArrayOf(1).inputStream() }
        archive.stage("remove", emptyMap()) { byteArrayOf(2).inputStream() }
        archive.discard("remove")
        assertEquals(listOf("keep"), archive.keys())
        assertArrayEquals(byteArrayOf(1), archive.payload("keep").readBytes())
    }

    @Test fun invalidKeysCannotEscapeTheArchiveDirectory() {
        val archive = TrashArchive(temporary.newFolder())
        listOf("../photo", "../../original", "/absolute", "a/b", "").forEach { key ->
            try { archive.payload(key); fail("Unsafe key was accepted: $key") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
