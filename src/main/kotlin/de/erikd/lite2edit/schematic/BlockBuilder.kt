package de.erikd.lite2edit.schematic

class BlockBuilder {
    private var pos: Vec3i = Vec3i.ZERO
    private var state: BlockState? = null

    fun position(x: Int, y: Int, z: Int) = apply { pos = Vec3i(x, y, z) }
    fun state(name: String, properties: Map<String, String> = emptyMap()) = apply {
        state = BlockState.of(name, properties)
    }

    fun air() = apply { state = null }

    fun build(): Block = Block(pos, state)
}