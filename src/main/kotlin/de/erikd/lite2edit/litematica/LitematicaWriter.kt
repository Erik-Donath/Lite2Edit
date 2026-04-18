package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.Schematic
import net.kyori.adventure.nbt.*

/*
[NBT STRUCTURE] Root compound (5 top‑level keys)
├─ MinecraftDataVersion (IntBinaryTagImpl)
├─ Version (IntBinaryTagImpl)
├─ Metadata (CompoundBinaryTagImpl) – 9 sub‑keys
│   ├─ TimeModified (LongBinaryTagImpl)
│   ├─ TimeCreated (LongBinaryTagImpl)
│   ├─ EnclosingSize (CompoundBinaryTagImpl) – 3 sub‑keys
│   │   ├─ x (IntBinaryTagImpl)
│   │   ├─ y (IntBinaryTagImpl)
│   │   └─ z (IntBinaryTagImpl)
│   ├─ Description (StringBinaryTagImpl)
│   ├─ TotalBlocks (IntBinaryTagImpl)
│   ├─ RegionCount (IntBinaryTagImpl)
│   ├─ TotalVolume (IntBinaryTagImpl)
│   ├─ Author (StringBinaryTagImpl)
│   └─ Name (StringBinaryTagImpl)
├─ Regions (CompoundBinaryTagImpl) – 1 sub‑keys
│   └─ Unnamed (CompoundBinaryTagImpl) – 8 sub‑keys
│       ├─ BlockStates (LongArrayBinaryTag) – 21735 elements
│       ├─ PendingBlockTicks (LIST<BinaryTagType[EndBinaryTag 0]>) – 0 elems
│       ├─ Position (CompoundBinaryTagImpl) – 3 sub‑keys
│       │   ├─ x (IntBinaryTagImpl)
│       │   ├─ y (IntBinaryTagImpl)
│       │   └─ z (IntBinaryTagImpl)
│       ├─ Size (CompoundBinaryTagImpl) – 3 sub‑keys
│       │   ├─ x (IntBinaryTagImpl)
│       │   ├─ y (IntBinaryTagImpl)
│       │   └─ z (IntBinaryTagImpl)
│       ├─ BlockStatePalette (LIST<BinaryTagType[CompoundBinaryTag 10]>) – 127 elems – first keys: Name
│       ├─ PendingFluidTicks (LIST<BinaryTagType[EndBinaryTag 0]>) – 0 elems
│       ├─ TileEntities (LIST<BinaryTagType[CompoundBinaryTag 10]>) – 8321 elems – first keys: x, y, z, id, OutputSignal
│       └─ Entities (LIST<BinaryTagType[EndBinaryTag 0]>) – 0 elems
└─ SubVersion (IntBinaryTagImpl)
 */

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