package de.erikd.lite2edit.util

import net.kyori.adventure.nbt.*
import org.enginehub.linbus.tree.*

// TO KYORI

// Scalar tags
fun LinByteTag.toKyori() = ByteBinaryTag.byteBinaryTag(value())
fun LinShortTag.toKyori() = ShortBinaryTag.shortBinaryTag(value())
fun LinIntTag.toKyori() = IntBinaryTag.intBinaryTag(value())
fun LinLongTag.toKyori() = LongBinaryTag.longBinaryTag(value())
fun LinFloatTag.toKyori() = FloatBinaryTag.floatBinaryTag(value())
fun LinDoubleTag.toKyori() = DoubleBinaryTag.doubleBinaryTag(value())
fun LinStringTag.toKyori() = StringBinaryTag.stringBinaryTag(value())

// Array tags
fun LinByteArrayTag.toKyori() = ByteArrayBinaryTag.byteArrayBinaryTag(*value())
fun LinIntArrayTag.toKyori() = IntArrayBinaryTag.intArrayBinaryTag(*value())
fun LinLongArrayTag.toKyori() = LongArrayBinaryTag.longArrayBinaryTag(*value())

// Complex tags
fun LinCompoundTag.toKyori(): CompoundBinaryTag {
    val builder = CompoundBinaryTag.builder()
    for ((key, tag) in value()) {
        builder.put(key, tag.toKyori())
    }
    return builder.build()
}

fun <T : LinTag<*>> LinListTag<T>.toKyori(): ListBinaryTag {
    val builder = ListBinaryTag.builder()
    for (tag in value()) {
        builder.add(tag.toKyori())
    }
    return builder.build()
}

// Generic conversion
fun LinTag<*>.toKyori(): BinaryTag = when (this) {
    is LinByteTag       -> toKyori()
    is LinShortTag      -> toKyori()
    is LinIntTag        -> toKyori()
    is LinLongTag       -> toKyori()
    is LinFloatTag      -> toKyori()
    is LinDoubleTag     -> toKyori()
    is LinStringTag     -> toKyori()
    is LinByteArrayTag  -> toKyori()
    is LinIntArrayTag   -> toKyori()
    is LinLongArrayTag  -> toKyori()
    is LinCompoundTag   -> toKyori()
    is LinListTag<*>    -> toKyori()
    else -> throw IllegalArgumentException("Unsupported Linbus tag type: ${this::class.java}")
}
