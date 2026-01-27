package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import de.erikd.lite2edit.worldedit.WEReader
import java.io.OutputStream

object WriteConverter {
    fun write(clipboard: Clipboard, stream: OutputStream?) {
        val schematic = WEReader.read(clipboard)
        println(schematic)
        TODO("Not yet implemented")
    }
}