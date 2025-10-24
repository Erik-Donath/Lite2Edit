package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream

class LitematicaSchematicFormat : ClipboardFormat {
    private val logger = LoggerFactory.getLogger(LitematicaSchematicFormat::class.java)

    override fun getName(): String = "Litematic Clipboard Format"
    override fun getAliases(): Set<String?> = setOf("litematic", "ltc")
    override fun getPrimaryFileExtension(): String = "litematic"
    override fun getFileExtensions(): Set<String?> = setOf("litematic", "ltc")

    override fun getReader(inputStream: InputStream?) = inputStream?.let { stream ->
        object : ClipboardReader {
            override fun read()  = LitematicaReadConverter.read(stream)
            override fun close() = stream.close()
        }
    } ?: throw IllegalArgumentException("InputStream cannot be null")

    override fun getWriter(outputStream: OutputStream?) = outputStream?.let { stream ->
        object : ClipboardWriter {
            override fun write(clipboard: Clipboard) = LitematicaWriteConverter.write(clipboard, stream)
            override fun close() = stream.close()
        }
    } ?: throw IllegalArgumentException("OutputStream cannot be null")
}
