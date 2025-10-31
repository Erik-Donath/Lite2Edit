package de.erikd.lite2edit

import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockState
import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.ByteArrayBinaryTag
import net.kyori.adventure.nbt.ByteBinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.DoubleBinaryTag
import net.kyori.adventure.nbt.FloatBinaryTag
import net.kyori.adventure.nbt.IntArrayBinaryTag
import net.kyori.adventure.nbt.IntBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import net.kyori.adventure.nbt.LongArrayBinaryTag
import net.kyori.adventure.nbt.LongBinaryTag
import net.kyori.adventure.nbt.ShortBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag
import org.enginehub.linbus.tree.LinCompoundTag
import org.enginehub.linbus.tree.LinIntTag
import org.enginehub.linbus.tree.LinListTag
import org.enginehub.linbus.tree.LinStringTag
import org.enginehub.linbus.tree.LinTagType
import org.slf4j.LoggerFactory

object TileEntityHelper {
    private val logger = LoggerFactory.getLogger(TileEntityHelper::class.java)

    fun createBaseBlock(blockState: BlockState, tileEntityNbt: CompoundBinaryTag?): BaseBlock {
        return if (tileEntityNbt == null) {
            blockState.toBaseBlock()
        } else {
            val linNbt = convertNbtToLinBus(tileEntityNbt)
            blockState.toBaseBlock(linNbt)
        }
    }

    private fun convertNbtToLinBus(nbt: CompoundBinaryTag): LinCompoundTag {
        val builder = LinCompoundTag.builder()
        for (key in nbt.keySet()) {
            try {
                when (val tag: BinaryTag? = nbt.get(key)) {
                    is ByteBinaryTag -> builder.putByte(key, tag.value().toInt().toByte())
                    is ShortBinaryTag -> builder.putShort(key, tag.value())
                    is IntBinaryTag -> builder.putInt(key, tag.value())
                    is LongBinaryTag -> builder.putLong(key, tag.value())
                    is FloatBinaryTag -> builder.putFloat(key, tag.value())
                    is DoubleBinaryTag -> builder.putDouble(key, tag.value())
                    is StringBinaryTag -> builder.putString(key, tag.value())
                    is ByteArrayBinaryTag -> builder.putByteArray(key, tag.value())
                    is IntArrayBinaryTag -> builder.putIntArray(key, tag.value())
                    is LongArrayBinaryTag -> builder.putLongArray(key, tag.value())
                    is CompoundBinaryTag -> builder.put(key, convertNbtToLinBus(tag))
                    is ListBinaryTag -> builder.put(key, convertNbtListToLinBus(tag))
                    else -> logger.warn("Unsupported NBT tag type for key $key: ${tag?.type()?.toString()}")
                }
            } catch (e: Exception) {
                logger.error("Failed to convert NBT tag '$key': ${e.message}", e)
            }
        }
        return builder.build()
    }

    private fun convertNbtListToLinBus(list: ListBinaryTag): LinListTag<*> {
        if (list.isEmpty) return LinListTag.builder(LinTagType.compoundTag()).build()

        return when (list.elementType()) {
            BinaryTagTypes.COMPOUND -> {
                val b = LinListTag.builder(LinTagType.compoundTag())
                for (i in 0 until list.size()) b.add(convertNbtToLinBus(list.getCompound(i)))
                b.build()
            }
            BinaryTagTypes.STRING -> {
                val b = LinListTag.builder(LinTagType.stringTag())
                for (i in 0 until list.size()) b.add(LinStringTag.of(list.getString(i)))
                b.build()
            }
            BinaryTagTypes.INT -> {
                val b = LinListTag.builder(LinTagType.intTag())
                for (i in 0 until list.size()) b.add(LinIntTag.of(list.getInt(i)))
                b.build()
            }
            else -> {
                logger.warn("Unsupported list element type: ${list.elementType()}")
                LinListTag.builder(LinTagType.compoundTag()).build()
            }
        }
    }

    fun createTileEntityMap(tileEntities: List<CompoundBinaryTag>): Map<String, CompoundBinaryTag> {
        return tileEntities.associateBy { nbt -> positionKey(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")) }
    }

    fun positionKey(x: Int, y: Int, z: Int): String = "$x,$y,$z"

    // TODO Add support for entity conversion not just tile entities
}
