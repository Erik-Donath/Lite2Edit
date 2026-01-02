package de.erikd.lite2edit.schematic

import net.kyori.adventure.nbt.CompoundBinaryTag

class EntityBuilder {
    private var nbt: CompoundBinaryTag = CompoundBinaryTag.empty()

    fun nbt(tag: CompoundBinaryTag) = apply { nbt = tag }

    fun build(): Entity = Entity(nbt)
}