package de.erikd.lite2edit.util

import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.CompoundBinaryTag
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.PushbackInputStream
import java.util.zip.GZIPInputStream

object NBTHelper {

    fun loadLitematic(input: InputStream): CompoundBinaryTag {
        val buffered = BufferedInputStream(input, 64 * 1024)
        val pb = PushbackInputStream(buffered, 4)
        try {
            // Detect gzip by magic 1F 8B
            val sig = ByteArray(2)
            val n = pb.read(sig)
            if (n > 0) pb.unread(sig, 0, n)
            val gz = n == 2 && sig[0] == 0x1f.toByte() && sig[1] == 0x8b.toByte()

            val decoded: InputStream = if (gz) GZIPInputStream(pb) else pb

            // Read without size limits
            val tag: BinaryTag = BinaryTagIO.unlimitedReader().read(decoded)

            val root = tag as? CompoundBinaryTag
                ?: throw IllegalArgumentException("Root tag is not a CompoundBinaryTag")

            return root
        } catch (t: Throwable) {
            throw RuntimeException("Failed to read Litematic NBT: ${t.message}", t)
        }
    }
}
