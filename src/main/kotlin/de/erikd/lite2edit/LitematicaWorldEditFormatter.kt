package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class LitematicaSchematicFormatter : ClipboardFormat {
    override fun getName(): String = "Litematica"
    override fun getAliases(): Set<String> = setOf("litematica", "litematic", "ltc")
    override fun getPrimaryFileExtension(): String = "litematic"
    override fun getFileExtensions(): Set<String> = setOf("litematic", "ltc")

    override fun isFormat(file: File?): Boolean =
        file?.name?.lowercase()?.let { name ->
            getFileExtensions().any { name.endsWith(".$it") }
        } == true

    override fun isFormat(inputStream: InputStream?): Boolean =
        inputStream?.let { stream ->
            runCatching {
                // Use only the first few bytes: check GZIP magic (1F 8B) and then
                // read just enough of the NBT header to verify the root compound
                // name contains a known Litematica key — far cheaper than a full parse.
                val buf = stream.readNBytes(256)
                if (buf.size < 3) return@runCatching false

                val isGzip = buf[0] == 0x1f.toByte() && buf[1] == 0x8b.toByte()
                if (!isGzip) return@runCatching false

                // Decompress the small header slice and look for Litematica root keys
                val inflated = java.util.zip.GZIPInputStream(buf.inputStream()).use { it.readBytes() }
                val header = String(inflated, Charsets.ISO_8859_1)
                // A valid Litematica file always contains all four of these keys
                listOf("Version", "SubVersion", "MinecraftDataVersion", "Metadata")
                    .all { header.contains(it) }
            }.getOrDefault(false)
        } ?: false

    override fun getReader(inputStream: InputStream?): ClipboardReader =
        inputStream?.let { stream ->
            object : ClipboardReader {
                override fun read()  = ReadConverter.read(stream)
                override fun close() = stream.close()
            }
        } ?: throw IllegalArgumentException("InputStream cannot be null")

    override fun getWriter(outputStream: OutputStream?): ClipboardWriter =
        outputStream?.let { stream ->
            object : ClipboardWriter {
                override fun write(clipboard: Clipboard) = WriteConverter.write(clipboard, stream)
                override fun close() = stream.close()
            }
        } ?: throw IllegalArgumentException("OutputStream cannot be null")
}