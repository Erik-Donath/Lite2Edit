@file:Suppress("unused")
package de.erikd.lite2edit.schematic

data class Schematic(
    val blocks: List<Block> = emptyList(),
    val entities: List<Entity> = emptyList(),
    val min: Vec3i,
    val max: Vec3i,
) {
    val size: Vec3i
        get() = Vec3i(
            dimension(min.x, max.x),
            dimension(min.y, max.y),
            dimension(min.z, max.z)
        )

    private fun dimension(minimum: Int, maximum: Int): Int {
        val size = maximum.toLong() - minimum + 1
        require(size in 0..Int.MAX_VALUE.toLong()) {
            "Schematic dimension is out of range: $minimum..$maximum"
        }
        return size.toInt()
    }
}