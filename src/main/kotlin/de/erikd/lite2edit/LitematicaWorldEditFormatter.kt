package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.GZIPInputStream

class LitematicaSchematicFormatter : ClipboardFormat {
    override fun getName(): String = FORMAT_NAME
    override fun getAliases(): Set<String> = ALIASES
    override fun getPrimaryFileExtension(): String = PRIMARY_EXTENSION
    override fun getFileExtensions(): Set<String> = FILE_EXTENSIONS

    override fun isFormat(file: File?): Boolean =
        file?.name?.lowercase(Locale.ROOT)?.let { name ->
            getFileExtensions().any { name.endsWith(".$it") }
        } == true

    override fun isFormat(inputStream: InputStream?): Boolean =
        inputStream?.let { stream ->
            try {
                val header = GZIPInputStream(stream).readNBytes(HEADER_SIZE)
                val headerText = String(header, Charsets.ISO_8859_1)
                header.firstOrNull() == COMPOUND_TAG_ID &&
                    IDENTIFYING_TAGS.all(headerText::contains)
            } catch (_: IOException) {
                false
            }
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

    private companion object {
        const val FORMAT_NAME = "Litematica"
        const val PRIMARY_EXTENSION = "litematic"
        const val HEADER_SIZE = 256
        const val COMPOUND_TAG_ID: Byte = 10
        val ALIASES = setOf("litematica", "litematic", "ltc")
        val FILE_EXTENSIONS = setOf("litematic", "ltc")
        val IDENTIFYING_TAGS = listOf("Version", "Metadata")
    }
}