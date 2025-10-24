package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class LitematicSchematicFormat : ClipboardFormat {

    private val logger = LoggerFactory.getLogger(LitematicSchematicFormat::class.java)

    override fun getName(): String = "Litematic Clipboard Format"
    override fun getAliases(): Set<String?> = setOf("litematic", "ltc")
    override fun getPrimaryFileExtension(): String = "litematic"
    override fun getFileExtensions(): Set<String?> = setOf("litematic", "ltc")

    override fun getReader(inputStream: InputStream?) = inputStream?.let { stream ->
        object : ClipboardReader {
            override fun read(): Clipboard {
                stream.use { s ->
                    val contentBytes = s.readBytes()
                    val content = contentBytes.toString(StandardCharsets.UTF_8)
                    logger.info("Reading clipboard content:\n$content")
                    // TODO: parse contentBytes to create a Clipboard instance
                    throw NotImplementedError("Clipboard parsing is not implemented yet")
                }
            }

            override fun close() = stream.close()
        }
    } ?: throw IllegalArgumentException("InputStream cannot be null")

    override fun getWriter(outputStream: OutputStream?) = outputStream?.let { stream ->
        object : ClipboardWriter {
            override fun write(clipboard: Clipboard) {
                stream.use { _ ->
                    logger.info("Writing clipboard content (serialization not implemented)")
                    // TODO: serialize clipboard object and write bytes to s
                    throw NotImplementedError("Clipboard writing is not implemented yet")
                }
            }

            override fun close() = stream.close()
        }
    } ?: throw IllegalArgumentException("OutputStream cannot be null")
}
