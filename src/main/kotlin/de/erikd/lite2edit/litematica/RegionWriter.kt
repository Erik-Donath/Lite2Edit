package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.Schematic
import net.kyori.adventure.nbt.*
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

object RegionWriter {

    private data class PaletteKey(val name: String, val properties: Map<String, String>)

    fun parse(schematic: Schematic): CompoundBinaryTag {
        val size   = schematic.size
        val offset = schematic.min

        val nonAirKeys = schematic.blocks
            .map  { PaletteKey(it.state.name, it.state.properties) }
            .distinct()
            .filterNot { it.name == "minecraft:air" }

        val paletteKeys: List<PaletteKey>   = listOf(PaletteKey("minecraft:air", emptyMap())) + nonAirKeys
        val paletteIndex: Map<PaletteKey, Int> = paletteKeys.mapIndexed { i, k -> k to i }.toMap()

        val totalBlocks = size.x * size.y * size.z
        val blockIds    = IntArray(totalBlocks) // default 0 = AIR

        for (block in schematic.blocks) {
            val rel   = block.pos - offset
            val index = rel.x + rel.z * size.x + rel.y * size.x * size.z
            if (index in 0 until totalBlocks)
                blockIds[index] = paletteIndex[PaletteKey(block.state.name, block.state.properties)] ?: 0
        }

        val encodedLongs = encodeBlockIds(blockIds, paletteKeys.size)

        val paletteNbt = ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
            for (key in paletteKeys) {
                val entry = CompoundBinaryTag.builder()
                    .put("Name", StringBinaryTag.stringBinaryTag(key.name))

                if (key.properties.isNotEmpty()) {
                    val props = CompoundBinaryTag.builder()
                    for ((k, v) in key.properties)
                        props.put(k, StringBinaryTag.stringBinaryTag(v))
                    entry.put("Properties", props.build())
                }

                add(entry.build())
            }
        }.build()

        val tileEntitiesNbt = ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
            for (block in schematic.blocks) {
                val nbt = block.state.nbt ?: continue
                val rel = block.pos - offset
                val entry = CompoundBinaryTag.builder()
                    .putInt("x", rel.x)
                    .putInt("y", rel.y)
                    .putInt("z", rel.z)
                nbt.keySet()
                    .filterNot { it == "x" || it == "y" || it == "z" }
                    .forEach { key -> entry.put(key, nbt.get(key)!!) }
                add(entry.build())
            }
        }.build()

        val entitiesNbt = if (schematic.entities.isEmpty()) {
            ListBinaryTag.empty()
        } else {
            ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
                schematic.entities.forEach { add(it.nbt) }
            }.build()
        }

        return CompoundBinaryTag.builder()
            .put("BlockStates",       LongArrayBinaryTag.longArrayBinaryTag(*encodedLongs))
            .put("PendingBlockTicks", ListBinaryTag.empty())
            .put("PendingFluidTicks", ListBinaryTag.empty())
            .put("Position",          offset.toLitematicaNbt())
            .put("Size",              size.toLitematicaNbt())
            .put("BlockStatePalette", paletteNbt)
            .put("TileEntities",      tileEntitiesNbt)
            .put("Entities",          entitiesNbt)
            .build()
    }

    /**
     * Encodes block IDs into Litematica's packed long-array format.
     * This is the exact inverse of [RegionReader.decodeBlockIds].
     * See: https://litemapy.readthedocs.io/en/latest/litematics.html
     */
    private fun encodeBlockIds(blockIds: IntArray, paletteSize: Int): LongArray {
        if (blockIds.isEmpty()) return LongArray(0)

        val bitsPerBlock = maxOf(2, ceil(log2(paletteSize.toDouble())).toInt())
        val totalBits    = blockIds.size * bitsPerBlock
        val longCount    = (totalBits + 63) / 64
        val result       = LongArray(longCount)

        var globalBitOffset = 0

        for (id in blockIds) {
            var remainingBits = bitsPerBlock
            var value         = id.toLong()

            while (remainingBits > 0) {
                val currentLongIdx = globalBitOffset / 64
                val localBitOffset = globalBitOffset % 64
                val bitsAvailable  = 64 - localBitOffset
                val bitsToWrite    = min(remainingBits, bitsAvailable)

                val mask = (1L shl bitsToWrite) - 1L
                result[currentLongIdx] = result[currentLongIdx] or ((value and mask) shl localBitOffset)

                value           = value ushr bitsToWrite
                remainingBits   -= bitsToWrite
                globalBitOffset += bitsToWrite
            }
        }

        return result
    }
}