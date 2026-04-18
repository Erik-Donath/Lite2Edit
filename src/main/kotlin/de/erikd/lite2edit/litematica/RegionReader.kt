package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.BlockBuilder
import de.erikd.lite2edit.schematic.SchematicBuilder
import de.erikd.lite2edit.schematic.Vec3i
import de.erikd.lite2edit.util.value
import net.kyori.adventure.nbt.*
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.min

object RegionReader {

    fun parse(builder: SchematicBuilder, regionTag: CompoundBinaryTag) {
        require(regionTag.contains("BlockStates"))      { "Missing BlockStates" }
        require(regionTag.contains("Position"))         { "Missing Position" }
        require(regionTag.contains("Size"))             { "Missing Size" }
        require(regionTag.contains("BlockStatePalette")){ "Missing BlockStatePalette" }
        require(regionTag.contains("TileEntities"))     { "Missing TileEntities" }
        require(regionTag.contains("Entities"))         { "Missing Entities" }

        val position = Vec3i.fromLitematicaNbt(regionTag.getCompound("Position"))
        val size     = Vec3i.fromLitematicaNbt(regionTag.getCompound("Size"))
        val sizeAbs  = size.abs()

        if (size == Vec3i.ZERO) return

        val offset = position + Vec3i(
            if (size.x < 0) size.x + 1 else 0,
            if (size.y < 0) size.y + 1 else 0,
            if (size.z < 0) size.z + 1 else 0
        )

        val palette: Map<Int, CompoundBinaryTag> =
            regionTag.getList("BlockStatePalette", BinaryTagTypes.COMPOUND)
                .mapIndexed { index, tag -> index to (tag as CompoundBinaryTag) }
                .toMap()

        val tileEntitiesMap: Map<Vec3i, CompoundBinaryTag> =
            regionTag.getList("TileEntities", BinaryTagTypes.COMPOUND)
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
            val name  = state.getString("Name")

            if (name == "minecraft:air") continue

            val properties: Map<String, String> = buildMap {
                state.getCompound("Properties").forEach { (key, tag) ->
                    this[key] = tag.value()?.toString() ?: ""
                }
            }

            builder.block(
                BlockBuilder()
                    .position(blockPos + offset)
                    .state(
                        name       = name,
                        properties = properties,
                        nbt        = tileEntitiesMap[blockPos]
                    )
                    .build()
            )
        }

        val entities = regionTag.getList("Entities", BinaryTagTypes.COMPOUND)
        for (entityTag in entities) {
            builder.entity(entityTag as CompoundBinaryTag)
        }
    }

    /**
     * Decodes the packed long-array block-state data used by Litematica.
     * See: https://litemapy.readthedocs.io/en/latest/litematics.html
     */
    private fun decodeBlockIds(longs: LongArray, paletteSize: Int, size: Vec3i): IntArray {
        val totalBlocks = size.x * size.y * size.z
        val result      = IntArray(totalBlocks)

        if (longs.isEmpty() || totalBlocks == 0 || paletteSize <= 0) return result

        val bitsPerBlock    = maxOf(2, ceil(log2(paletteSize.toDouble())).toInt())
        var globalBitOffset = 0
        var currentLongIdx  = 0

        for (i in 0 until totalBlocks) {
            var remainingBits = bitsPerBlock
            var value         = 0L
            var valueShift    = 0

            while (remainingBits > 0 && currentLongIdx < longs.size) {
                val localBitOffset = globalBitOffset % 64
                val bitsAvailable  = 64 - localBitOffset
                val bitsToTake     = min(remainingBits, bitsAvailable)

                val mask = (1L shl bitsToTake) - 1L
                val part = (longs[currentLongIdx] ushr localBitOffset) and mask

                value         = value or (part shl valueShift)
                valueShift    += bitsToTake
                remainingBits -= bitsToTake
                globalBitOffset += bitsToTake

                if (globalBitOffset % 64 == 0) currentLongIdx++
            }

            result[i] = value.toInt()
        }

        return result
    }
}