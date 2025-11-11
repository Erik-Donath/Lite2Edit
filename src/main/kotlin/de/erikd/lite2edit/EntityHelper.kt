package de.erikd.lite2edit

import com.sk89q.worldedit.entity.BaseEntity
import net.kyori.adventure.nbt.*
import org.enginehub.linbus.tree.*
import org.slf4j.LoggerFactory

object EntityHelper {
    private val logger = LoggerFactory.getLogger(EntityHelper::class.java)

    fun convertLitematicaEntity(nbt: CompoundBinaryTag): BaseEntity? {
        val id = nbt.getString("id")
        if (id.isEmpty()) {
            logger.warn("Litematica entity missing 'id'; skipping")
            return null
        }

        val type = com.sk89q.worldedit.world.entity.EntityTypes.get(id)
        if (type == null) {
            logger.warn("Unknown Entity type: $id; skipping")
            return null
        }

        val entity = BaseEntity(type)
        entity.nbt = convertNbtToLinBus(nbt)

        return entity
    }

    fun convertNbtToLinBus(nbt: CompoundBinaryTag): LinCompoundTag {
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

    fun convertNbtListToLinBus(list: ListBinaryTag): LinListTag<out LinTag<*>> {
        if (list.isEmpty()) return LinListTag.builder(LinTagType.compoundTag()).build()
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
            BinaryTagTypes.FLOAT -> {
                val b = LinListTag.builder(LinTagType.floatTag())
                for (i in 0 until list.size()) b.add(org.enginehub.linbus.tree.LinFloatTag.of(list.getFloat(i)))
                b.build()
            }
            BinaryTagTypes.DOUBLE -> {
                val b = LinListTag.builder(LinTagType.doubleTag())
                for (i in 0 until list.size()) b.add(org.enginehub.linbus.tree.LinDoubleTag.of(list.getDouble(i)))
                b.build()
            }
            BinaryTagTypes.LONG -> {
                val b = LinListTag.builder(LinTagType.longTag())
                for (i in 0 until list.size()) b.add(org.enginehub.linbus.tree.LinLongTag.of(list.getLong(i)))
                b.build()
            }
            BinaryTagTypes.BYTE -> {
                val b = LinListTag.builder(LinTagType.byteTag())
                for (i in 0 until list.size()) b.add(org.enginehub.linbus.tree.LinByteTag.of(list.getByte(i)))
                b.build()
            }
            BinaryTagTypes.SHORT -> {
                val b = LinListTag.builder(LinTagType.shortTag())
                for (i in 0 until list.size()) b.add(org.enginehub.linbus.tree.LinShortTag.of(list.getShort(i)))
                b.build()
            }
            else -> {
                logger.warn("Unsupported list element type: ${list.elementType()}")
                LinListTag.builder(LinTagType.compoundTag()).build()
            }
        }
    }
}
