package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import de.erikd.lite2edit.litematica.FileHelper
import de.erikd.lite2edit.litematica.LitematicaReader
import de.erikd.lite2edit.worldedit.WEWriter
import java.io.InputStream

object ReadConverter {
    @JvmStatic
    fun read(input: InputStream): Clipboard {
        val rootTag = FileHelper.loadLitematic(input)
        val schematic = LitematicaReader.read(rootTag)
        return WEWriter.write(schematic)
    }
}