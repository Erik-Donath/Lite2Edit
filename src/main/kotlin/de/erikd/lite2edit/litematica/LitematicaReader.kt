package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.SchematicBuilder
import net.kyori.adventure.nbt.CompoundBinaryTag
import org.slf4j.LoggerFactory

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
object LitematicaReader {
    private val logger = LoggerFactory.getLogger(LitematicaReader::class.java)

    @JvmStatic
    fun read(root: CompoundBinaryTag): de.erikd.lite2edit.schematic.Schematic {
        require(root.contains("Regions")) { "Missing Regions compound" }

        // We do not use:
        //require(root.contains("Metadata")) { "Missing Metadata compound" }
        //require(root.contains("MinecraftDataVersion")) { "Missing MinecraftDataVersion compound" }
        //require(root.contains("Version")) { "Missing Version compound" }
        //require(root.contains("SubVersion")) { "Missing SubVersion compound" }

        val builder = SchematicBuilder()
        val regions = root.getCompound("Regions")

        for (regionName in regions.keySet()) {
            val regionTag = regions.getCompound(regionName)
            RegionReader.parse(builder, regionTag)
        }

        return builder.build()
    }
}