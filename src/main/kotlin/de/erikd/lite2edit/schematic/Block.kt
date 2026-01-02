package de.erikd.lite2edit.schematic

data class Block(
    val pos: Vec3i,
    val state: BlockState? = null
) {
    val isAir: Boolean get() = state == null
}