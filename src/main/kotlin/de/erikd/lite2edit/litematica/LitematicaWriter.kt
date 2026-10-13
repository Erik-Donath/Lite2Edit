package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.Schematic
import net.kyori.adventure.nbt.*

object LitematicaWriter {
    private const val VERSION      = 6
    private const val SUB_VERSION  = 1
    private const val DESCRIPTION  = "Thanks for using Lite2Edit"

    private const val DATA_VERSION = 3700

    fun write(schematic: Schematic): CompoundBinaryTag {
        val size   = schematic.size
        val now    = System.currentTimeMillis()
        val region = RegionWriter.parse(schematic)

        return CompoundBinaryTag.builder()
            .put("MinecraftDataVersion", IntBinaryTag.intBinaryTag(DATA_VERSION))
            .put("Version",              IntBinaryTag.intBinaryTag(VERSION))
            .put("SubVersion",           IntBinaryTag.intBinaryTag(SUB_VERSION))
            .put("Metadata", CompoundBinaryTag.builder()
                .put("TimeModified",  LongBinaryTag.longBinaryTag(now))
                .put("TimeCreated",   LongBinaryTag.longBinaryTag(now))
                .put("EnclosingSize", size.toLitematicaNbt())
                .put("Description",   StringBinaryTag.stringBinaryTag(DESCRIPTION))
                .put("TotalBlocks",   IntBinaryTag.intBinaryTag(schematic.blocks.count { !it.isAir }))
                .put("RegionCount",   IntBinaryTag.intBinaryTag(1))
                .put("TotalVolume",   IntBinaryTag.intBinaryTag(size.volume().toInt()))
                .put("Author",        StringBinaryTag.stringBinaryTag("Lite2Edit"))
                .put("Name",          StringBinaryTag.stringBinaryTag("Lite2Edit Export"))
                .build()
            )
            .put("Regions", CompoundBinaryTag.builder()
                .put("Unnamed", region)
                .build()
            )
            .build()
    }
}