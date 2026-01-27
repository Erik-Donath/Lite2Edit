package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import de.erikd.lite2edit.litematica.FileHelper
import de.erikd.lite2edit.litematica.LitematicaWriter
import de.erikd.lite2edit.worldedit.WEReader
import java.io.OutputStream

object WriteConverter {
    fun write(clipboard: Clipboard, stream: OutputStream) {
        val schematic = WEReader.read(clipboard)
        println(schematic)
        val root = LitematicaWriter.write(schematic)
        println(root)
        FileHelper.saveLitematic(root, stream)
    }
}