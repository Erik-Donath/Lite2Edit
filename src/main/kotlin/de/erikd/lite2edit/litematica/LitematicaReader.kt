package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.Schematic
import de.erikd.lite2edit.schematic.SchematicBuilder
import net.kyori.adventure.nbt.CompoundBinaryTag

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