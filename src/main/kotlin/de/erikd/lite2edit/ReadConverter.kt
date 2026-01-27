package de.erikd.lite2edit

import de.erikd.lite2edit.litematica.FileHelper          // GZIP‑aware NBT loader
import de.erikd.lite2edit.litematica.LitematicaReader    // Turns NBT → Schematic
import de.erikd.lite2edit.worldedit.WEWriter             // Turns Schematic → Clipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import java.io.InputStream

object ReadConverter {
    @JvmStatic
    fun read(input: InputStream): Clipboard {
        val rootTag = FileHelper.loadLitematic(input)
        val schematic = LitematicaReader.read(rootTag)
        return WEWriter.write(schematic)
    }
}