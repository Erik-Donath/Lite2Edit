package de.erikd.lite2edit

import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockState
import de.erikd.lite2edit.EntityHelper.convertNbtToLinBus
import net.kyori.adventure.nbt.CompoundBinaryTag

object TileEntityHelper {
    fun createBaseBlock(blockState: BlockState, tileEntityNbt: CompoundBinaryTag?): BaseBlock {
        return if (tileEntityNbt == null) {
            blockState.toBaseBlock()
        } else {
            val linNbt = convertNbtToLinBus(tileEntityNbt)
            blockState.toBaseBlock(linNbt)
        }
    }

    fun createTileEntityMap(tileEntities: List<CompoundBinaryTag>): Map<String, CompoundBinaryTag> {
        return tileEntities.associateBy { nbt -> positionKey(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")) }
    }

    fun positionKey(x: Int, y: Int, z: Int): String = "$x,$y,$z"

    // TODO Add support for entity conversion not just tile entities
}
