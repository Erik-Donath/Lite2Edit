package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.Schematic
import de.erikd.lite2edit.schematic.Vec3i
import net.kyori.adventure.nbt.*

object RegionWriter {

    private data class PaletteKey(val name: String, val properties: Map<String, String>)

    fun parse(schematic: Schematic): CompoundBinaryTag {
        val size   = schematic.size
        val offset = schematic.min

        val paletteIndex = linkedMapOf(PaletteKey("minecraft:air", emptyMap()) to 0)
        for (block in schematic.blocks) {
            val key = PaletteKey(block.state.name, block.state.properties)
            if (key.name != "minecraft:air") paletteIndex.getOrPut(key) { paletteIndex.size }
        }
        val paletteKeys = paletteIndex.keys.toList()

        val totalBlocks = if (size == Vec3i.ZERO) {
            0
        } else {
            BlockStatePacking.blockCount(size)
        }
        val blockIds = IntArray(totalBlocks)
        for (block in schematic.blocks) {
            val rel   = block.pos - offset
            if (rel.x !in 0 until size.x || rel.y !in 0 until size.y || rel.z !in 0 until size.z) continue

            val index = rel.x.toLong() +
                rel.z.toLong() * size.x +
                rel.y.toLong() * size.x * size.z
            blockIds[index.toInt()] = paletteIndex[PaletteKey(block.state.name, block.state.properties)] ?: 0
        }

        val encodedLongs = BlockStatePacking.encode(blockIds, paletteKeys.size)

        val paletteNbt = ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
            for (key in paletteKeys) {
                val entry = CompoundBinaryTag.builder()
                    .put("Name", StringBinaryTag.stringBinaryTag(key.name))

                if (key.properties.isNotEmpty()) {
                    val props = CompoundBinaryTag.builder()
                    for ((k, v) in key.properties)
                        props.put(k, StringBinaryTag.stringBinaryTag(v))
                    entry.put("Properties", props.build())
                }

                add(entry.build())
            }
        }.build()

        val tileEntitiesNbt = ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
            for (block in schematic.blocks) {
                val nbt = block.state.nbt ?: continue
                val rel = block.pos - offset
                val entry = CompoundBinaryTag.builder()
                    .putInt("x", rel.x)
                    .putInt("y", rel.y)
                    .putInt("z", rel.z)
                nbt.keySet()
                    .filterNot { it == "x" || it == "y" || it == "z" }
                    .forEach { key -> nbt.get(key)?.let { entry.put(key, it) } }
                add(entry.build())
            }
        }.build()

        val entitiesNbt = if (schematic.entities.isEmpty()) {
            ListBinaryTag.empty()
        } else {
            ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
                schematic.entities.forEach { add(it.nbt) }
            }.build()
        }

        return CompoundBinaryTag.builder()
            .put("BlockStates",       LongArrayBinaryTag.longArrayBinaryTag(*encodedLongs))
            .put("PendingBlockTicks", ListBinaryTag.empty())
            .put("PendingFluidTicks", ListBinaryTag.empty())
            .put("Position",          offset.toLitematicaNbt())
            .put("Size",              size.toLitematicaNbt())
            .put("BlockStatePalette", paletteNbt)
            .put("TileEntities",      tileEntitiesNbt)
            .put("Entities",          entitiesNbt)
            .build()
    }
}