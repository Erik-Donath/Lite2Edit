@file:Suppress("unused")
package de.erikd.lite2edit.schematic

class SchematicBuilder {
    private val blockList = mutableListOf<Block>()
    private val entityList = mutableListOf<Entity>()

    private var min: Vec3i? = null
    private var max: Vec3i? = null

    fun block(pos: Vec3i, state: BlockState) = apply {
        blockList.add(Block(pos, state))
        updateBounds(pos)
    }

    fun airBlock(pos: Vec3i) = apply {
        blockList.add(Block(pos, BlockState.AIR))
        updateBounds(pos)
    }

    fun block(block: Block) = apply {
        blockList.add(block)
        updateBounds(block.pos)
    }

    fun entity(nbt: net.kyori.adventure.nbt.CompoundBinaryTag) = apply {
        entityList.add(Entity(nbt))
    }

    fun entity(entity: Entity) = apply {
        entityList.add(entity)
    }

    private fun updateBounds(pos: Vec3i) {
        if (min == null) {
            min = pos
            max = pos
        } else {
            min = Vec3i(
                minOf(min!!.x, pos.x),
                minOf(min!!.y, pos.y),
                minOf(min!!.z, pos.z)
            )
            max = Vec3i(
                maxOf(max!!.x, pos.x),
                maxOf(max!!.y, pos.y),
                maxOf(max!!.z, pos.z)
            )
        }
    }

    fun build(): Schematic = Schematic(
        blocks = blockList.toList(),
        entities = entityList.toList(),
        min = min ?: Vec3i.ZERO,
        max = max ?: (Vec3i.ZERO - Vec3i.ONE)
    )
}
