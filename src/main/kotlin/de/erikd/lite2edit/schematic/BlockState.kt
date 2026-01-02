package de.erikd.lite2edit.schematic

import net.kyori.adventure.nbt.CompoundBinaryTag

data class BlockState(
    val name: String,
    val properties: Map<String, String> = emptyMap(),
    val nbt: CompoundBinaryTag = CompoundBinaryTag.empty()
) {
    companion object {
        fun of(name: String, properties: Map<String, String> = emptyMap()) =
            BlockState(name, properties, CompoundBinaryTag.empty())
    }
}