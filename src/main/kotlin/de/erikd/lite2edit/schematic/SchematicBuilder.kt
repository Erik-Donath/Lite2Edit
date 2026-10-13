@file:Suppress("unused")
package de.erikd.lite2edit.schematic

class SchematicBuilder {
    private val blockList  = mutableListOf<Block>()
    private val entityList = mutableListOf<Entity>()

    private var min: Vec3i? = null
    private var max: Vec3i? = null

    fun block(pos: Vec3i, state: BlockState) = block(Block(pos, state))

    fun block(block: Block) = apply {
        blockList.add(block)
        if (!block.isAir) updateBounds(block.pos)
    }

    fun entity(nbt: net.kyori.adventure.nbt.CompoundBinaryTag) = apply {
        entityList.add(Entity(nbt))
    }

    fun entity(entity: Entity) = apply {
        entityList.add(entity)
    }

    private fun updateBounds(pos: Vec3i) {
        val currentMin = min
        val currentMax = max
        if (currentMin == null || currentMax == null) {
            min = pos
            max = pos
        } else {
            min = Vec3i(minOf(currentMin.x, pos.x), minOf(currentMin.y, pos.y), minOf(currentMin.z, pos.z))
            max = Vec3i(maxOf(currentMax.x, pos.x), maxOf(currentMax.y, pos.y), maxOf(currentMax.z, pos.z))
        }
    }

    fun build(): Schematic = Schematic(
        blocks   = blockList.toList(),
        entities = entityList.toList(),
        min      = min ?: Vec3i.ZERO,
        max      = max ?: (Vec3i.ZERO - Vec3i.ONE)
    )
}