package de.erikd.lite2edit.schematic

data class Schematic(
    val metadata: Map<String, String> = emptyMap(),
    val blocks: List<Block> = emptyList(),
    val entities: List<Entity> = emptyList()
) {
    fun getBlockAt(pos: Vec3i): Block? = blockIndex[pos]
    fun nonAirBlockCount(): Long = blocks.count { !it.isAir }.toLong()

    private val blockIndex: Map<Vec3i, Block> by lazy {
        blocks.associateBy { it.pos }
    }

    fun blocksAtY(y: Int): List<Block> = blocks.filter { it.pos.y == y }
    fun entitiesAtY(y: Int): List<Entity> = entities.filter { entityY(it) == y }

    private fun entityY(entity: Entity): Int? {
        val posList = entity.nbt.getList("Pos", net.kyori.adventure.nbt.BinaryTagTypes.DOUBLE) ?: return null
        return posList.getDouble(1).toInt()
    }
}