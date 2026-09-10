package org.hndrx.loamgallery.data

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.security.DigestInputStream
import java.security.MessageDigest

/** Durable, app-private recovery copies for Android 8–10. No original is removed here. */
class TrashArchive(private val root: File) {
    private fun file(key: String, suffix: String): File {
        require(key.matches(Regex("[a-zA-Z0-9-]{1,64}")))
        check(root.isDirectory || root.mkdirs())
        return File(root, "$key.$suffix")
    }

    fun payload(key: String): File = file(key, "media")

    fun stage(key: String, metadata: Map<String, String>, source: () -> InputStream) {
        if (file(key, "properties").exists() && payload(key).exists()) {
            val saved = read(key)
            check(payload(key).length() == saved.getValue("copiedBytes").toLong()) { "Recovery copy size changed" }
            check(checksum(key) == saved.getValue("sha256")) { "Recovery copy verification failed" }
            return
        }
        val partial = file(key, "part")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val count = DigestInputStream(source(), digest).use { input -> FileOutputStream(partial).use { output ->
                val copied = input.copyTo(output)
                output.fd.sync()
                copied
            } }
            val expected = metadata["size"]?.toLongOrNull() ?: 0
            check(count > 0 && (expected <= 0 || count == expected)) { "Incomplete recovery copy" }
            Files.move(partial.toPath(), payload(key).toPath(), StandardCopyOption.REPLACE_EXISTING)
            write(key, metadata + mapOf("state" to "prepared", "copiedBytes" to count.toString(), "sha256" to digest.digest().joinToString("") { "%02x".format(it) }))
        } finally { partial.delete() }
    }

    fun write(key: String, metadata: Map<String, String>) {
        val temporary = file(key, "tmp")
        FileOutputStream(temporary).use { output ->
            Properties().apply { putAll(metadata) }.store(output, "Loam recycle bin")
            output.fd.sync()
        }
        Files.move(temporary.toPath(), file(key, "properties").toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    fun read(key: String): Map<String, String> = file(key, "properties").inputStream().use { input ->
        Properties().apply { load(input) }.entries.associate { it.key.toString() to it.value.toString() }
    }

    fun verify(key: String) {
        val saved = read(key)
        check(payload(key).length() == saved.getValue("copiedBytes").toLong() && checksum(key) == saved.getValue("sha256")) { "Recovery copy verification failed" }
    }

    private fun checksum(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(payload(key).inputStream(), digest).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (input.read(buffer) >= 0) { /* Stream without retaining media in memory. */ }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun keys(): List<String> = root.listFiles().orEmpty().filter { it.extension == "properties" && payload(it.nameWithoutExtension).exists() }.map { it.nameWithoutExtension }

    fun discard(key: String) {
        // Keep the manifest if a payload could not be removed, so it remains recoverable.
        val data = payload(key)
        check(!data.exists() || data.delete()) { "Could not remove recovery copy" }
        val metadata = file(key, "properties")
        check(!metadata.exists() || metadata.delete()) { "Could not remove recovery metadata" }
    }
}
