package de.erikd.lite2edit.worldedit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.world.block.BlockTypes
import de.erikd.lite2edit.schematic.BlockBuilder
import de.erikd.lite2edit.schematic.Schematic
import de.erikd.lite2edit.schematic.SchematicBuilder
import de.erikd.lite2edit.schematic.Vec3i
import de.erikd.lite2edit.util.toKyori
import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.DoubleBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag

object WEReader {
    private val AIR_TYPES = setOfNotNull(
        BlockTypes.AIR,
        BlockTypes.CAVE_AIR,
        BlockTypes.VOID_AIR
    )

    @JvmStatic
    fun read(clipboard: Clipboard): Schematic {
        val builder = SchematicBuilder()

        // --- Blocks ----------------------------------------------------------
        blockPositions(clipboard.region)
            .filterNot { pos -> clipboard.getBlock(pos).blockType in AIR_TYPES }
            .forEach { pos ->
                val baseBlock = clipboard.getFullBlock(pos)
                builder.block(
                    BlockBuilder()
                        .position(Vec3i.fromBlockVector3(pos))
                        .state(
                            baseBlock.blockType.id,
                            baseBlock.states.entries.associate { (prop, value) -> prop.name to value.toString() },
                            baseBlock.nbt?.toKyori()
                        )
                        .build()
                )
            }

        clipboard.entities.forEach { entity ->
            val state = entity.state ?: return@forEach
            val loc   = entity.location

            // Merge WorldEdit entity state into Litematica-compatible NBT:
            val existingNbt = state.nbt?.toKyori() ?: CompoundBinaryTag.empty()

            val entityNbt = CompoundBinaryTag.builder()
                .put("id", StringBinaryTag.stringBinaryTag(state.type.id()))
                .put("Pos", ListBinaryTag.builder(BinaryTagTypes.DOUBLE)
                    .add(DoubleBinaryTag.doubleBinaryTag(loc.x))
                    .add(DoubleBinaryTag.doubleBinaryTag(loc.y))
                    .add(DoubleBinaryTag.doubleBinaryTag(loc.z))
                    .build()
                )

            // Copy any remaining NBT keys (skip id/Pos — already set above)
            existingNbt.keySet()
                .filterNot { it == "id" || it == "Pos" }
                .forEach { key -> existingNbt.get(key)?.let { tag -> entityNbt.put(key, tag) } }

            builder.entity(entityNbt.build())
        }

        return builder.build()
    }

    private fun blockPositions(region: Region): Sequence<BlockVector3> = sequence {
        val min = region.minimumPoint
        val max = region.maximumPoint
        for (x in min.x()..max.x())
            for (y in min.y()..max.y())
                for (z in min.z()..max.z())
                    yield(BlockVector3.at(x, y, z))
    }
}