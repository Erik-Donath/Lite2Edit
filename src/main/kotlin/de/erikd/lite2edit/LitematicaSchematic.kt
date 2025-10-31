package de.erikd.lite2edit

import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag

import kotlin.math.abs
import kotlin.math.max

/**
 * Litematica File Format Structure:
 *
 * Root NBT (GZIP compressed):
 * ├─ MinecraftDataVersion: Int (e.g., 2975 for 1.20.4)
 * ├─ Version: Int (Litematica format version)
 * ├─ Metadata: Compound
 * │ ├─ Name: String (schematic name)
 * │ ├─ Author: String
 * │ ├─ Description: String
 * │ └─ TimeCreated/Modified: Long
 * └─ Regions: Compound (can contain multiple named regions)
 *    └─ <regionName>: Compound
 *       ├─ Position: Compound {x, y, z} (offset in world)
 *       ├─ Size: Compound {x, y, z} (dimensions, can be negative!)
 *       ├─ BlockStatePalette: List
 *       │  └─ Entry: Compound
 *       │     ├─ Name: String (e.g., "minecraft:stone")
 *       │     └─ Properties: Compound (optional, e.g., {facing: "north"})
 *       ├─ BlockStates: LongArray (bit-packed palette indices)
 *       ├─ TileEntities: List (chests, signs, etc.)
 *       │  └─ Entry: Compound {x, y, z, id, ...custom NBT}
 *       └─ Entities: List (mobs, armor stands, etc.)
 *          └─ Entry: Compound {Pos, id, ...custom NBT}
 *
 * Block Storage Format:
 * - Blocks are stored in a bit-packed long array (BlockStates)
 * - Each block is represented by a palette index
 * - Bits per block = max(2, ceil(log2(palette_size)))
 * - Iteration order: X (inner) -> Z -> Y (outer)
 * - Negative dimensions mean the region extends in negative direction
 */
class LitematicaSchematic(
    val minecraftDataVersion: Int = 0,
    val version: Int = 0,
    val metadata: Metadata = Metadata(),
    val regions: Map<String, Region> = emptyMap()
) {

    constructor(nbt: CompoundBinaryTag) : this(
        minecraftDataVersion = nbt.getInt("MinecraftDataVersion"),
        version = nbt.getInt("Version"),
        metadata = Metadata(nbt.getCompound("Metadata")),
        regions = parseRegions(nbt.getCompound("Regions"))
    )

    private companion object {
        private fun parseRegions(regionsTag: CompoundBinaryTag): Map<String, Region> {
            val keys = regionsTag.keySet()
            val out = LinkedHashMap<String, Region>(keys.size)
            for (key in keys) {
                out[key] = Region(regionsTag.getCompound(key))
            }
            return out
        }

        private fun getCompoundList(nbt: CompoundBinaryTag, key: String): ListBinaryTag {
            return nbt.getList(key, BinaryTagTypes.COMPOUND)
        }
    }

    fun toNbt(): CompoundBinaryTag = CompoundBinaryTag.builder().apply {
        putInt("MinecraftDataVersion", minecraftDataVersion)
        putInt("Version", version)
        put("Metadata", metadata.toNbt())
        val regionsBuilder = CompoundBinaryTag.builder()
        regions.forEach { (name, region) ->
            regionsBuilder.put(name, region.toNbt())
        }
        put("Regions", regionsBuilder.build())
    }.build()

    data class Metadata(
        val name: String = "",
        val author: String = "",
        val description: String = "",
        val timeCreated: Long = 0L,
        val timeModified: Long = 0L
    ) {
        constructor(nbt: CompoundBinaryTag) : this(
            name = nbt.getString("Name"),
            author = nbt.getString("Author"),
            description = nbt.getString("Description"),
            timeCreated = nbt.getLong("TimeCreated"),
            timeModified = nbt.getLong("TimeModified")
        )

        fun toNbt(): CompoundBinaryTag = CompoundBinaryTag.builder().apply {
            putString("Name", name)
            putString("Author", author)
            putString("Description", description)
            putLong("TimeCreated", timeCreated)
            putLong("TimeModified", timeModified)
        }.build()
    }

    data class Region(
        val position: Vec3 = Vec3(),
        val size: Vec3 = Vec3(),
        val blockStatePalette: List<BlockStateEntry> = emptyList(),
        var blockStates: LongArray = longArrayOf(),
        val tileEntities: MutableList<CompoundBinaryTag> = mutableListOf(),
        val entities: MutableList<CompoundBinaryTag> = mutableListOf()
    ) {

        constructor(nbt: CompoundBinaryTag) : this(
            position = Vec3(nbt.getCompound("Position")),
            size = Vec3(nbt.getCompound("Size")),
            blockStatePalette = parsePalette(nbt),
            blockStates = nbt.getLongArray("BlockStates"),
            tileEntities = parseCompoundList(nbt, "TileEntities"),
            entities = parseCompoundList(nbt, "Entities")
        )

        private companion object {
            private fun parsePalette(nbt: CompoundBinaryTag): List<BlockStateEntry> {
                val lb: ListBinaryTag = nbt.getList("BlockStatePalette", BinaryTagTypes.COMPOUND)
                val out = ArrayList<BlockStateEntry>(lb.size())
                for (i in 0 until lb.size()) {
                    out.add(BlockStateEntry(lb.getCompound(i)))
                }
                return out
            }

            private fun parseCompoundList(nbt: CompoundBinaryTag, key: String): MutableList<CompoundBinaryTag> {
                val lb: ListBinaryTag = nbt.getList(key, BinaryTagTypes.COMPOUND)
                val out = ArrayList<CompoundBinaryTag>(lb.size())
                for (i in 0 until lb.size()) {
                    out.add(lb.getCompound(i))
                }
                return out
            }
        }

        private val bitsPerBlock: Int by lazy {
            max(2, 32 - Integer.numberOfLeadingZeros(blockStatePalette.size - 1))
        }

        private val numBlocks: Int by lazy {
            abs(size.x * size.y * size.z)
        }

        val offset: Vec3
            get() = Vec3(
                position.x + if (size.x < 0) size.x + 1 else 0,
                position.y + if (size.y < 0) size.y + 1 else 0,
                position.z + if (size.z < 0) size.z + 1 else 0
            )

        fun decodeBlocks(): IntArray {
            val result = IntArray(numBlocks)
            val mask = (1L shl bitsPerBlock) - 1L
            for (i in 0 until numBlocks) {
                val bitOffset = i * bitsPerBlock
                val longIndex = bitOffset / 64
                val bitIndex = bitOffset % 64
                val current = blockStates.getOrNull(longIndex) ?: 0L
                val value = if (bitIndex + bitsPerBlock <= 64) {
                    (current shr bitIndex) and mask
                } else {
                    val part1 = current ushr bitIndex
                    val part2 = (blockStates.getOrNull(longIndex + 1) ?: 0L) shl (64 - bitIndex)
                    (part1 or part2) and mask
                }
                result[i] = value.toInt()
            }
            return result
        }

        fun getBlockIndex(x: Int, y: Int, z: Int): Int {
            val (nx, ny, nz) = normalizeCoords(x, y, z)
            if (nx !in 0 until abs(size.x) ||
                ny !in 0 until abs(size.y) ||
                nz !in 0 until abs(size.z)
            ) return -1
            val index = ny * abs(size.x) * abs(size.z) + nz * abs(size.x) + nx
            val decoded = decodeBlocks()
            return decoded.getOrElse(index) { -1 }
        }

        fun setBlockIndex(x: Int, y: Int, z: Int, blockIndex: Int) {
            val (nx, ny, nz) = normalizeCoords(x, y, z)
            require(nx in 0 until abs(size.x)) { "x out of bounds" }
            require(ny in 0 until abs(size.y)) { "y out of bounds" }
            require(nz in 0 until abs(size.z)) { "z out of bounds" }
            require(blockIndex in blockStatePalette.indices) { "blockIndex out of palette range" }

            val index = ny * abs(size.x) * abs(size.z) + nz * abs(size.x) + nx

            val mask = (1L shl bitsPerBlock) - 1L
            val bitOffset = index * bitsPerBlock
            val longIndex = bitOffset / 64
            val bitIndex = bitOffset % 64

            // Create mutable copy of blockStates (if necessary)
            val mutableStates = blockStates.toMutableList()

            // Clear old bits, then set new bits for blockIndex
            val current = mutableStates.getOrElse(longIndex) { 0L }
            val cleared = current and (mask shl bitIndex).inv()
            mutableStates[longIndex] = cleared or ((blockIndex.toLong() and mask) shl bitIndex)

            if (bitIndex + bitsPerBlock > 64) {
                val overlapBits = bitIndex + bitsPerBlock - 64
                val nextIndex = longIndex + 1
                val nextCurrent = mutableStates.getOrElse(nextIndex) { 0L }
                val clearedNext = nextCurrent and ((1L shl overlapBits) - 1).inv()
                mutableStates[nextIndex] =
                    clearedNext or ((blockIndex.toLong() shr (bitsPerBlock - overlapBits)) and ((1L shl overlapBits) - 1))
            }

            blockStates = mutableStates.toLongArray()
        }

        private fun normalizeCoords(x: Int, y: Int, z: Int): Triple<Int, Int, Int> {
            val nx = if (size.x < 0) size.x + x else x
            val ny = if (size.y < 0) size.y + y else y
            val nz = if (size.z < 0) size.z + z else z
            return Triple(nx, ny, nz)
        }

        fun blocksWithCoordinates() = sequence {
            val decoded = decodeBlocks()
            val sizeX = abs(size.x)
            val sizeY = abs(size.y)
            val sizeZ = abs(size.z)
            for (i in decoded.indices) {
                val x = i % sizeX
                val z = (i / sizeX) % sizeZ
                val y = i / (sizeX * sizeZ)
                yield(Quad(x, y, z, decoded[i]))
            }
        }

        fun findTileEntity(x: Int, y: Int, z: Int): CompoundBinaryTag? {
            return tileEntities.firstOrNull {
                it.getInt("x") == x && it.getInt("y") == y && it.getInt("z") == z
            }
        }

        fun setTileEntity(tileEntityNbt: CompoundBinaryTag) {
            val x = tileEntityNbt.getInt("x")
            val y = tileEntityNbt.getInt("y")
            val z = tileEntityNbt.getInt("z")
            val index = tileEntities.indexOfFirst {
                it.getInt("x") == x && it.getInt("y") == y && it.getInt("z") == z
            }
            if (index == -1) {
                tileEntities.add(tileEntityNbt)
            } else {
                tileEntities[index] = tileEntityNbt
            }
        }

        fun removeTileEntity(x: Int, y: Int, z: Int): Boolean {
            return tileEntities.removeIf {
                it.getInt("x") == x && it.getInt("y") == y && it.getInt("z") == z
            }
        }

        fun tileEntitiesSequence() = tileEntities.asSequence()
        fun entitiesSequence() = entities.asSequence()

        fun toNbt(): CompoundBinaryTag = CompoundBinaryTag.builder().apply {
            put("Position", position.toNbt())
            put("Size", this@Region.size.toNbt())

            val paletteList = ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
                blockStatePalette.forEach { add(it.toNbt()) }
            }.build()
            put("BlockStatePalette", paletteList)

            putLongArray("BlockStates", blockStates)

            val tileList = ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
                tileEntities.forEach { add(it) }
            }.build()
            put("TileEntities", tileList)

            val entityList = ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
                entities.forEach { add(it) }
            }.build()
            put("Entities", entityList)
        }.build()

        data class BlockStateEntry(
            val name: String = "",
            val properties: CompoundBinaryTag = CompoundBinaryTag.empty()
        ) {
            constructor(nbt: CompoundBinaryTag) : this(
                name = nbt.getString("Name"),
                properties = nbt.getCompound("Properties")
            )

            fun toNbt(): CompoundBinaryTag = CompoundBinaryTag.builder().apply {
                putString("Name", name)
                put("Properties", properties)
            }.build()
        }

        data class Quad(val x: Int, val y: Int, val z: Int, val paletteIndex: Int)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Region

            if (position != other.position) return false
            if (size != other.size) return false
            if (blockStatePalette != other.blockStatePalette) return false
            if (!blockStates.contentEquals(other.blockStates)) return false
            if (tileEntities != other.tileEntities) return false
            if (entities != other.entities) return false
            if (bitsPerBlock != other.bitsPerBlock) return false
            if (numBlocks != other.numBlocks) return false
            if (offset != other.offset) return false

            return true
        }

        override fun hashCode(): Int {
            var result = position.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + blockStatePalette.hashCode()
            result = 31 * result + blockStates.contentHashCode()
            result = 31 * result + tileEntities.hashCode()
            result = 31 * result + entities.hashCode()
            result = 31 * result + bitsPerBlock
            result = 31 * result + numBlocks
            result = 31 * result + offset.hashCode()
            return result
        }
    }

    data class Vec3(val x: Int = 0, val y: Int = 0, val z: Int = 0) {
        constructor(nbt: CompoundBinaryTag) : this(
            x = nbt.getInt("x"),
            y = nbt.getInt("y"),
            z = nbt.getInt("z")
        )

        fun toNbt(): CompoundBinaryTag = CompoundBinaryTag.builder().apply {
            putInt("x", x)
            putInt("y", y)
            putInt("z", z)
        }.build()
    }
}
