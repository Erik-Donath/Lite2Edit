package de.erikd.lite2edit

import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockState
import net.minecraft.nbt.NbtCompound
import org.enginehub.linbus.tree.LinCompoundTag
import org.enginehub.linbus.tree.LinListTag
import org.enginehub.linbus.tree.LinStringTag
import org.enginehub.linbus.tree.LinIntTag
import org.enginehub.linbus.tree.LinTagType
import org.slf4j.LoggerFactory

object TileEntityHelper {
    private val logger = LoggerFactory.getLogger(TileEntityHelper::class.java)

    fun createBaseBlock(blockState: BlockState, tileEntityNbt: NbtCompound?): BaseBlock {
        return if (tileEntityNbt == null) {
            blockState.toBaseBlock()
        } else {
            val linNbt = convertNbtToLinBus(tileEntityNbt)
            blockState.toBaseBlock(linNbt)
        }
    }

    private fun convertNbtToLinBus(nbt: NbtCompound): LinCompoundTag {
        val builder = LinCompoundTag.builder()

        for (key in nbt.keys) {
            try {
                when (val tag = nbt.get(key)) {
                    is net.minecraft.nbt.NbtByte -> builder.putByte(key, tag.byteValue())
                    is net.minecraft.nbt.NbtShort -> builder.putShort(key, tag.shortValue())
                    is net.minecraft.nbt.NbtInt -> builder.putInt(key, tag.intValue())
                    is net.minecraft.nbt.NbtLong -> builder.putLong(key, tag.longValue())
                    is net.minecraft.nbt.NbtFloat -> builder.putFloat(key, tag.floatValue())
                    is net.minecraft.nbt.NbtDouble -> builder.putDouble(key, tag.doubleValue())
                    is net.minecraft.nbt.NbtString -> builder.putString(key, tag.asString())
                    is net.minecraft.nbt.NbtByteArray -> builder.putByteArray(key, tag.byteArray)
                    is net.minecraft.nbt.NbtIntArray -> builder.putIntArray(key, tag.intArray)
                    is net.minecraft.nbt.NbtLongArray -> builder.putLongArray(key, tag.longArray)
                    is net.minecraft.nbt.NbtCompound -> builder.put(key, convertNbtToLinBus(tag))
                    is net.minecraft.nbt.NbtList -> builder.put(key, convertNbtListToLinBus(tag))
                    else -> logger.warn("Unsupported NBT tag type for key '$key': ${tag?.javaClass?.simpleName}")
                }
            } catch (e: Exception) {
                logger.error("Failed to convert NBT tag '$key': ${e.message}", e)
            }
        }

        return builder.build()
    }

    private fun convertNbtListToLinBus(list: net.minecraft.nbt.NbtList): LinListTag<*> {
        if (list.isEmpty()) {
            return LinListTag.builder(LinTagType.compoundTag()).build()
        }

        return when (val firstElement = list[0]) {
            is net.minecraft.nbt.NbtCompound -> {
                val builder = LinListTag.builder(LinTagType.compoundTag())
                for (i in 0 until list.size) {
                    builder.add(convertNbtToLinBus(list.getCompound(i)))
                }
                builder.build()
            }
            is net.minecraft.nbt.NbtString -> {
                val builder = LinListTag.builder(LinTagType.stringTag())
                for (i in 0 until list.size) {
                    builder.add(LinStringTag.of(list.getString(i)))
                }
                builder.build()
            }
            is net.minecraft.nbt.NbtInt -> {
                val builder = LinListTag.builder(LinTagType.intTag())
                for (i in 0 until list.size) {
                    builder.add(LinIntTag.of(list.getInt(i)))
                }
                builder.build()
            }
            else -> {
                logger.warn("Unsupported list element type: ${firstElement.javaClass.simpleName}")
                LinListTag.builder(LinTagType.compoundTag()).build()
            }
        }
    }

    fun createTileEntityMap(tileEntities: List<NbtCompound>): Map<String, NbtCompound> = tileEntities.associateBy {nbt ->
        positionKey(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"))
    }

    fun positionKey(x: Int, y: Int, z: Int): String = "$x,$y,$z"

    // TODO: Add support for entity conversion (not just tile entities)
}
