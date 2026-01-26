package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.Vec3i
import net.kyori.adventure.nbt.CompoundBinaryTag

internal fun Vec3i.toLitematicaNbt(): CompoundBinaryTag = CompoundBinaryTag.builder()
    .putInt("x", x)
    .putInt("y", y)
    .putInt("z", z)
    .build()

internal fun Vec3i.Companion.fromLitematicaNbt(tag: CompoundBinaryTag): Vec3i = Vec3i(
    x = tag.getInt("x"),
    y = tag.getInt("y"),
    z = tag.getInt("z")
)