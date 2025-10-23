package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter
import java.io.InputStream
import java.io.OutputStream

class LitematicSchematicFormat : ClipboardFormat {
    override fun getName(): String = "litematic"
    override fun getAliases(): Set<String?> = setOf("litematic")
    override fun getPrimaryFileExtension(): String = "litematic"
    override fun getFileExtensions(): Set<String?> = setOf("litematic")

    override fun getReader(inputStream: InputStream?): ClipboardReader {
        println("Reading litematic")
        TODO("Not yet implemented")
    }

    override fun getWriter(outputStream: OutputStream?): ClipboardWriter {
        println("Write litematic")
        TODO("Not yet implemented")
    }
}