package de.erikd.lite2edit.util

import net.kyori.adventure.nbt.*
import org.enginehub.linbus.tree.*

// TO LINBUS

// Scalar tags
fun ByteBinaryTag.toLinbus()   = LinByteTag.of(value())
fun ShortBinaryTag.toLinbus()  = LinShortTag.of(value())
fun IntBinaryTag.toLinbus()    = LinIntTag.of(value())
fun LongBinaryTag.toLinbus()   = LinLongTag.of(value())
fun FloatBinaryTag.toLinbus()  = LinFloatTag.of(value())
fun DoubleBinaryTag.toLinbus() = LinDoubleTag.of(value())
fun StringBinaryTag.toLinbus() = LinStringTag.of(value())

// Array tags
fun ByteArrayBinaryTag.toLinbus()  = LinByteArrayTag.of(*value())
fun IntArrayBinaryTag.toLinbus()   = LinIntArrayTag.of(*value())
fun LongArrayBinaryTag.toLinbus()  = LinLongArrayTag.of(*value())

// Complex tags
fun CompoundBinaryTag.toLinbus(): LinCompoundTag {
    val builder = LinCompoundTag.builder()
    keySet().forEach { key -> builder.put(key, get(key)!!.toLinbus()) }
    return builder.build()
}

fun ListBinaryTag.toLinbus(): LinListTag<out LinTag<*>> {
    if (size() == 0) return LinListTag.builder(LinTagType.byteTag()).build()

    return when (val elementType = elementType()) {
        BinaryTagTypes.BYTE       -> buildList(this) { LinListTag.builder(LinTagType.byteTag()) }
        BinaryTagTypes.SHORT      -> buildList(this) { LinListTag.builder(LinTagType.shortTag()) }
        BinaryTagTypes.INT        -> buildList(this) { LinListTag.builder(LinTagType.intTag()) }
        BinaryTagTypes.LONG       -> buildList(this) { LinListTag.builder(LinTagType.longTag()) }
        BinaryTagTypes.FLOAT      -> buildList(this) { LinListTag.builder(LinTagType.floatTag()) }
        BinaryTagTypes.DOUBLE     -> buildList(this) { LinListTag.builder(LinTagType.doubleTag()) }
        BinaryTagTypes.STRING     -> buildList(this) { LinListTag.builder(LinTagType.stringTag()) }
        BinaryTagTypes.COMPOUND   -> buildList(this) { LinListTag.builder(LinTagType.compoundTag()) }
        BinaryTagTypes.LIST       -> buildList(this) { LinListTag.builder(LinTagType.listTag<LinByteTag>()) }
        BinaryTagTypes.BYTE_ARRAY -> buildList(this) { LinListTag.builder(LinTagType.byteArrayTag()) }
        BinaryTagTypes.INT_ARRAY  -> buildList(this) { LinListTag.builder(LinTagType.intArrayTag()) }
        BinaryTagTypes.LONG_ARRAY -> buildList(this) { LinListTag.builder(LinTagType.longArrayTag()) }
        else -> throw IllegalArgumentException("Unsupported list element type: $elementType")
    }
}

fun BinaryTag.toLinbus(): LinTag<*> = when (this) {
    is ByteBinaryTag      -> toLinbus()
    is ShortBinaryTag     -> toLinbus()
    is IntBinaryTag       -> toLinbus()
    is LongBinaryTag      -> toLinbus()
    is FloatBinaryTag     -> toLinbus()
    is DoubleBinaryTag    -> toLinbus()
    is StringBinaryTag    -> toLinbus()
    is ByteArrayBinaryTag -> toLinbus()
    is IntArrayBinaryTag  -> toLinbus()
    is LongArrayBinaryTag -> toLinbus()
    is CompoundBinaryTag  -> toLinbus()
    is ListBinaryTag      -> toLinbus()
    else -> throw IllegalArgumentException("Unsupported tag type: ${this::class.java}")
}

private inline fun <reified T : LinTag<*>> buildList(
    list: ListBinaryTag,
    createBuilder: () -> LinListTag.Builder<T>
): LinListTag<out LinTag<*>> {
    val builder = createBuilder()
    @Suppress("UNCHECKED_CAST")
    val mutable = builder as LinListTag.Builder<LinTag<Any>>
    for (i in 0 until list.size()) {
        @Suppress("UNCHECKED_CAST")
        mutable.add(list.get(i).toLinbus() as LinTag<Any>)
    }
    return mutable.build()
}

// Access Values

fun BinaryTag.value(): Any? = when (this) {
    is ByteBinaryTag      -> value()
    is ShortBinaryTag     -> value()
    is IntBinaryTag       -> value()
    is LongBinaryTag      -> value()
    is FloatBinaryTag     -> value()
    is DoubleBinaryTag    -> value()
    is StringBinaryTag    -> value()
    is ByteArrayBinaryTag -> value()
    is IntArrayBinaryTag  -> value()
    is LongArrayBinaryTag -> value()
    is CompoundBinaryTag  -> null
    is ListBinaryTag      -> null
    else -> throw IllegalArgumentException("No value(): ${this::class.java}")
}