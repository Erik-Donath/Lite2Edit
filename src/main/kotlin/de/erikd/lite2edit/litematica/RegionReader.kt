package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.BlockBuilder
import de.erikd.lite2edit.schematic.SchematicBuilder
import de.erikd.lite2edit.schematic.Vec3i
import de.erikd.lite2edit.util.value
import net.kyori.adventure.nbt.*

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

        if (size == Vec3i.ZERO) return

        val sizeAbs = Vec3i(
            absoluteDimension(size.x),
            absoluteDimension(size.y),
            absoluteDimension(size.z)
        )
        val totalBlocks = BlockStatePacking.blockCount(sizeAbs)
        val offset = position + Vec3i(
            if (size.x < 0) size.x + 1 else 0,
            if (size.y < 0) size.y + 1 else 0,
            if (size.z < 0) size.z + 1 else 0
        )

        val palette = regionTag.getList("BlockStatePalette", BinaryTagTypes.COMPOUND)

        val tileEntitiesMap: Map<Vec3i, CompoundBinaryTag> =
            regionTag.getList("TileEntities", BinaryTagTypes.COMPOUND)
                .map { it as CompoundBinaryTag }
                .associateBy { Vec3i.fromLitematicaNbt(it) }

        val blockIds = BlockStatePacking.decode(
            regionTag.getLongArray("BlockStates"),
            palette.size(),
            totalBlocks
        )

        for (i in blockIds.indices) {
            val id = blockIds[i]
            val blockPos = Vec3i(
                x = i % sizeAbs.x,
                z = (i / sizeAbs.x) % sizeAbs.z,
                y = i / (sizeAbs.x * sizeAbs.z)
            )

            if (id !in 0 until palette.size()) continue
            val state = palette[id] as CompoundBinaryTag
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

    private fun absoluteDimension(dimension: Int): Int {
        require(dimension != Int.MIN_VALUE) { "Region dimension is out of range: $dimension" }
        return kotlin.math.abs(dimension)
    }
}