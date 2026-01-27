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

object WEReader {
    @JvmStatic
    fun read(clipboard: Clipboard): Schematic {
        val builder = SchematicBuilder()

        blockPositions(clipboard.region)
            .filterNot {
                pos -> clipboard.getBlock(pos).blockType === BlockTypes.AIR
            }
            .forEach { pos ->
                val baseBlock = clipboard.getFullBlock(pos)

                builder.block(BlockBuilder()
                    .position(Vec3i.fromBlockVector3(pos))
                    .state(
                        baseBlock.blockType.id,
                        baseBlock.states.mapKeys { it.key.name },
                        baseBlock.nbt?.toKyori()
                    )
                    .build()
                )
            }
        return builder.build()
    }

    private fun blockPositions(region: Region): Sequence<BlockVector3> = sequence {
        val min = region.minimumPoint
        val max = region.maximumPoint

        for (x in min.x()..max.x()) {
            for (y in min.y()..max.y()) {
                for (z in min.z()..max.z()) {
                    yield(BlockVector3.at(x, y, z))
                }
            }
        }
    }
}