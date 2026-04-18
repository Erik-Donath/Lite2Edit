@file:Suppress("unused")
package de.erikd.lite2edit.schematic

import net.kyori.adventure.nbt.CompoundBinaryTag

class BlockBuilder {
    private var pos: Vec3i = Vec3i.ZERO
    private var state: BlockState = BlockState.AIR

    fun position(x: Int, y: Int, z: Int) = apply { pos = Vec3i(x, y, z) }
    fun position(position : Vec3i) = apply { pos = position }
    fun state(name: String, properties: Map<String, String> = emptyMap(), nbt: CompoundBinaryTag? = null) = apply {
        state = BlockState(name, properties, nbt)
    }

    fun air() = apply { state = BlockState.AIR }

    fun build(): Block = Block(pos, state)
}