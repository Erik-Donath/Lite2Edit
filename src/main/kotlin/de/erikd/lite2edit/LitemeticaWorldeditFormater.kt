package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter
import de.erikd.lite2edit.litematica.FileHelper
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class LitematicaSchematicFormater : ClipboardFormat {
    override fun getName(): String = "Litematica"
    override fun getAliases(): Set<String> = setOf("litematica", "litematic", "ltc")
    override fun getPrimaryFileExtension(): String = "litematic"
    override fun getFileExtensions(): Set<String> = setOf("litematic", "ltc")

    override fun isFormat(file: File?): Boolean =
        file?.name?.lowercase()?.let { name ->
            getFileExtensions().any {
                name.endsWith(".$it")
            }
        } == true

    override fun isFormat(inputStream: InputStream?): Boolean =
        inputStream?.let {
            runCatching {
                FileHelper.loadLitematic(it)
            }
            .getOrNull()
            ?.let { root ->
                root.contains("Version") &&
                root.contains("SubVersion") &&
                root.contains("MinecraftDataVersion") &&
                root.contains("Metadata")
                } == true
        } ?: false

    override fun getReader(inputStream: InputStream?) = inputStream?.let { stream ->
        object : ClipboardReader {
            override fun read()  = ReadConverter.read(stream)
            override fun close() = stream.close()
        }
    } ?: throw IllegalArgumentException("InputStream cannot be null")

    override fun getWriter(outputStream: OutputStream?) = outputStream?.let { stream ->
        object : ClipboardWriter {
            override fun write(clipboard: Clipboard) = WriteConverter.write(clipboard, stream)
            override fun close() = stream.close()
        }
    } ?: throw IllegalArgumentException("OutputStream cannot be null")
}
