package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.BlockBuilder
import de.erikd.lite2edit.schematic.SchematicBuilder
import de.erikd.lite2edit.schematic.Vec3i
import de.erikd.lite2edit.util.value
import net.kyori.adventure.nbt.*
import net.minecraft.world.phys.Vec3
import kotlin.collections.forEach
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.min


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

object RegionReader {

    fun parse(builder: SchematicBuilder, regionTag: CompoundBinaryTag) {
        require(regionTag.contains("BlockStates")) { "Missing BlockStates compound" }
        require(regionTag.contains("Position")) { "Missing Position compound" }
        require(regionTag.contains("Size")) { "Missing Size compound" }
        require(regionTag.contains("BlockStatePalette")) { "Missing BlockStatePalette compound" }
        require(regionTag.contains("TileEntities")) { "Missing TileEntities compound" }
        require(regionTag.contains("Entities")) { "Missing Entities compound" }

        // We do not use:
        //require(regionTag.contains("PendingBlockTicks")) { "Missing PendingBlockTicks compound" }
        //require(regionTag.contains("PendingFluidTicks")) { "Missing PendingFluidTicks compound" }

        val position = Vec3i.fromLitematicaNbt(regionTag.getCompound("Position"))
        val size = Vec3i.fromLitematicaNbt(regionTag.getCompound("Size"))
        val sizeAbs = size.abs()
        val offset = position + Vec3i(
            if (size.x < 0) size.x + 1 else 0,
            if (size.y < 0) size.y + 1 else 0,
            if (size.z < 0) size.z + 1 else 0
        )

        if(size == Vec3.ZERO) {
            return
        }

        val palette: Map<Int, CompoundBinaryTag> =
            regionTag.getList("BlockStatePalette", BinaryTagTypes.COMPOUND)
                .mapIndexed { index, tag -> index to (tag as CompoundBinaryTag) }
                .toMap()

        val tileEntities = regionTag.getList("TileEntities", BinaryTagTypes.COMPOUND)
        val tileEntitiesMap: Map<Vec3i, CompoundBinaryTag> = tileEntities
                .map { it as CompoundBinaryTag }
                .associateBy { Vec3i.fromLitematicaNbt(it) }

        val blockIds = decodeBlockIds(regionTag.getLongArray("BlockStates"), palette.size, sizeAbs)

        for (i in blockIds.indices) {
            val id = blockIds[i]
            val blockPos = Vec3i(
                x = i % sizeAbs.x,
                z = (i / sizeAbs.x) % sizeAbs.z,
                y = i / (sizeAbs.x * sizeAbs.z)
            )

            val state = palette[id] ?: continue
            val nbt = tileEntitiesMap[blockPos]
            val name = state.getString("Name")
            val properties: Map<String, Any?> = buildMap {
                state.getCompound("Properties").forEach { (key, value) ->
                    this[key] = value.value()
                }
            }

            if (name == "minecraft:air") continue // Skip air blocks

            builder.block(BlockBuilder()
                .position(blockPos + offset)
                .state(
                    name = name,
                    properties = properties,
                    nbt = nbt
                )
                .build()
            )
        }

        //TODO("Add Entity support")
    }

    private fun decodeBlockIds(
        longs: LongArray,
        paletteSize: Int,
        size: Vec3i
    ): IntArray {
        // Please read the litematica documentation to understand what is going on: https://litemapy.readthedocs.io/en/latest/litematics.html

        val totalBlocks = size.x * size.y * size.z
        val result = IntArray(totalBlocks)

        if (longs.isEmpty() || totalBlocks == 0 || paletteSize <= 0) return result

        val bitsPerBlock = maxOf(2, ceil(log2(paletteSize.toDouble())).toInt())
        var globalBitOffset = 0
        var currentLongIndex = 0

        for (i in 0 until totalBlocks) {
            var remainingBits = bitsPerBlock
            var value = 0L
            var valueShift = 0

            while (remainingBits > 0 && currentLongIndex < longs.size) {
                val currentLong = longs[currentLongIndex]
                val localBitOffset = globalBitOffset % 64
                val bitsAvailable = 64 - localBitOffset
                val bitsToTake = min(remainingBits, bitsAvailable)

                val mask = (1L shl bitsToTake) - 1L
                val part = (currentLong ushr localBitOffset) and mask

                value = value or (part shl valueShift)

                valueShift += bitsToTake
                remainingBits -= bitsToTake
                globalBitOffset += bitsToTake

                if (globalBitOffset % 64 == 0) {
                    currentLongIndex++
                }
            }

            result[i] = value.toInt()
        }

        return result
    }
}