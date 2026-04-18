package de.erikd.lite2edit.schematic

import net.kyori.adventure.nbt.CompoundBinaryTag

data class BlockState(
    val name: String,
    val properties: Map<String, String> = emptyMap(),
    val nbt: CompoundBinaryTag? = null
) {
    companion object {
        val AIR = BlockState("minecraft:air")
    }
}