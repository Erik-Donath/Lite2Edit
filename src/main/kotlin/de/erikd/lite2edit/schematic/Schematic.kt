@file:Suppress("unused")
package de.erikd.lite2edit.schematic

data class Schematic(
    val blocks: List<Block> = emptyList(),
    val entities: List<Entity> = emptyList(),
    val min: Vec3i,
    val max: Vec3i,
) {
    fun size(): Vec3i = max - min + Vec3i.ONE
}