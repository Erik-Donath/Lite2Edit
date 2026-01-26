@file:Suppress("UnusedSymbol")
package de.erikd.lite2edit.schematic

data class Block(
    val pos: Vec3i,
    val state: BlockState = BlockState.AIR
) {
    val isAir: Boolean get() = state == BlockState.AIR
}