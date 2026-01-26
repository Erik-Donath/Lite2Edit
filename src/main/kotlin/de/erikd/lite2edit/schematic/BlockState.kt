package de.erikd.lite2edit.schematic

import net.kyori.adventure.nbt.CompoundBinaryTag

data class BlockState(
    val name: String,
    val properties: Map<String, Any?> = emptyMap(),
    val nbt: CompoundBinaryTag? = null
) {
    companion object {
        fun of(name: String, properties: Map<String, Any?> = emptyMap(), nbt: CompoundBinaryTag? = null): BlockState =
            BlockState(name, properties, nbt)

        val AIR = BlockState("minecraft:air")
    }
}