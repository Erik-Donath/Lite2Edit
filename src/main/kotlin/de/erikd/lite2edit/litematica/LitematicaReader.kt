package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.Schematic
import de.erikd.lite2edit.schematic.SchematicBuilder
import net.kyori.adventure.nbt.CompoundBinaryTag

object LitematicaReader {
    @JvmStatic
    fun read(root: CompoundBinaryTag): Schematic {
        require(root.contains("Regions")) { "Missing Regions compound" }

        // Metadata, MinecraftDataVersion, Version and SubVersion are intentionally not
        // required here: Lite2Edit only needs the Regions data to reconstruct blocks and
        // entities, so partial or minimally-valid files can still be imported.

        val builder = SchematicBuilder()
        val regions = root.getCompound("Regions")

        for (regionName in regions.keySet()) {
            val regionTag = regions.getCompound(regionName)
            RegionReader.parse(builder, regionTag)
        }

        return builder.build()
    }
}