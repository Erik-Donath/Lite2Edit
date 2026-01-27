package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.Schematic
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.IntBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import net.kyori.adventure.nbt.LongArrayBinaryTag
import net.kyori.adventure.nbt.LongBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag

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
    const val LITEMATIC_VERSION = 6
    const val LITEMATIC_SUBVERSION = 1
    const val LITEMATIC_DATA_VERSION = 3700
    const val LITEMATIC_DESCRIPTIONS = "Thanks for using Lite2Edit"

    fun write(schematic: Schematic): CompoundBinaryTag {
        val size = schematic.size()

        val builder = CompoundBinaryTag.builder()
            .put("MinecraftDataVersion", IntBinaryTag.intBinaryTag(LITEMATIC_DATA_VERSION))
            .put("Version", IntBinaryTag.intBinaryTag(LITEMATIC_VERSION))
            .put("SubVersion", IntBinaryTag.intBinaryTag(LITEMATIC_SUBVERSION))
            .put("Metadata", CompoundBinaryTag.builder()
                .put("TimeModified", LongBinaryTag.longBinaryTag(System.currentTimeMillis()))
                .put("TimeCreated", LongBinaryTag.longBinaryTag(System.currentTimeMillis()))
                .put("EnclosingSize", size.toLitematicaNbt())
                .put("Description", StringBinaryTag.stringBinaryTag(LITEMATIC_DESCRIPTIONS))
                .put("TotalBlocks", IntBinaryTag.intBinaryTag(size.total()))
                .put("RegionCount", IntBinaryTag.intBinaryTag(1))
                .put("TotalVolume", IntBinaryTag.intBinaryTag(size.total()))
                .put("Author", StringBinaryTag.stringBinaryTag("Lite2Edit"))
                .put("Name", StringBinaryTag.stringBinaryTag("unnamed"))
                .build()
            )

        val region = CompoundBinaryTag.builder()
            .put("BlockStates", LongArrayBinaryTag.longArrayBinaryTag())
            .put("PendingBlockTicks", ListBinaryTag.empty())
            .put("PendingFluidTicks", ListBinaryTag.empty())
            .put("Position", schematic.min.toLitematicaNbt())
            .put("Size", size.toLitematicaNbt())
            .put("BlockStatePalette", ListBinaryTag.empty())
            .put("TileEntities", ListBinaryTag.empty())
            .put("Entities", ListBinaryTag.empty())


        builder.put("Regions", CompoundBinaryTag.builder()
            .put("Unnamed", region.build())
            .build()
        )

        return builder.build()
    }
}