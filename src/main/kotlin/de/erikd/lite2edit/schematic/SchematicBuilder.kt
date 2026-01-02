package de.erikd.lite2edit.schematic

import java.util.*

class SchematicBuilder {

    private val meta = mutableMapOf<String, String>()
    private val blockList = mutableListOf<Block>()
    private val entityList = mutableListOf<Entity>()

    fun metadata(key: String, value: String) = apply { meta[key] = value }

    fun metadata(map: Map<String, String>) = apply { meta.putAll(map) }

    fun block(pos: Vec3i, state: BlockState) = apply {
        blockList.add(Block(pos, state))
    }

    fun airBlock(pos: Vec3i) = apply {
        blockList.add(Block(pos, null))
    }

    fun block(block: Block) = apply { blockList.add(block) }

    fun entity(nbt: net.kyori.adventure.nbt.CompoundBinaryTag) = apply {
        entityList.add(Entity(nbt))
    }

    fun entity(entity: Entity) = apply { entityList.add(entity) }

    fun build(): Schematic = Schematic(
        metadata = Collections.unmodifiableMap(meta),
        blocks = Collections.unmodifiableList(blockList),
        entities = Collections.unmodifiableList(entityList)
    )
}