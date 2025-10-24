package de.erikd.lite2edit

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import kotlin.math.abs
import kotlin.math.max

/**
 * Litematica File Format Structure:
 *
 * Root NBT (GZIP compressed):
 * ├─ MinecraftDataVersion: Int (e.g., 2975 for 1.20.4)
 * ├─ Version: Int (Litematica format version)
 * ├─ Metadata: Compound
 * │  ├─ Name: String (schematic name)
 * │  ├─ Author: String
 * │  ├─ Description: String
 * │  └─ TimeCreated/Modified: Long
 * └─ Regions: Compound (can contain multiple named regions)
 *    └─ <RegionName>: Compound
 *       ├─ Position: Compound {x, y, z} (offset in world)
 *       ├─ Size: Compound {x, y, z} (dimensions, can be negative!)
 *       ├─ BlockStatePalette: List<Compound>
 *       │  └─ Entry: Compound
 *       │     ├─ Name: String (e.g., "minecraft:stone")
 *       │     └─ Properties: Compound (optional, e.g., {facing: "north"})
 *       ├─ BlockStates: LongArray (bit-packed palette indices)
 *       ├─ TileEntities: List<Compound> (chests, signs, etc.)
 *       │  └─ Entry: Compound {x, y, z, id, ...custom NBT}
 *       └─ Entities: List<Compound> (mobs, armor stands, etc.)
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
    constructor(nbt: NbtCompound) : this(
        minecraftDataVersion = nbt.getInt("MinecraftDataVersion"),
        version = nbt.getInt("Version"),
        metadata = Metadata(nbt.getCompound("Metadata")),
        regions = nbt.getCompound("Regions").keys.associateWith {
            Region(nbt.getCompound("Regions").getCompound(it))
        }
    )

    fun toNbt(): NbtCompound = NbtCompound().apply {
        putInt("MinecraftDataVersion", minecraftDataVersion)
        putInt("Version", version)
        put("Metadata", metadata.toNbt())
        val regionsCompound = NbtCompound()
        regions.forEach { (name, region) ->
            regionsCompound.put(name, region.toNbt())
        }
        put("Regions", regionsCompound)
    }

    /**
     * Iterates over all blocks in all regions, invoking the given action with
     * the region name, block coordinates, palette index, and the block state entry.
     */
    fun forEachBlock(action: (regionName: String, x: Int, y: Int, z: Int, blockIndex: Int, blockStateEntry: Region.BlockStateEntry) -> Unit) {
        regions.forEach { (regionName, region) ->
            region.blocksWithCoordinates().forEach { (x, y, z, idx) ->
                if (idx in region.blockStatePalette.indices) {
                    action(regionName, x, y, z, idx, region.blockStatePalette[idx])
                }
            }
        }
    }

    /**
     * Returns a sequence of all blocks with their full context (regionName, coordinates, palette index, block state) across all regions.
     */
    fun blocksSequence() = sequence {
        regions.forEach { (regionName, region) ->
            region.blocksWithCoordinates().forEach { (x, y, z, idx) ->
                if (idx in region.blockStatePalette.indices) {
                    yield(RegionBlock(regionName, x, y, z, idx, region.blockStatePalette[idx]))
                }
            }
        }
    }

    /**
     * Helper data class representing a block with coordinate and palette info
     */
    data class RegionBlock(
        val regionName: String,
        val x: Int,
        val y: Int,
        val z: Int,
        val paletteIndex: Int,
        val blockStateEntry: Region.BlockStateEntry
    )

    data class Metadata(
        val name: String = "",
        val author: String = "",
        val description: String = "",
        val timeCreated: Long = 0L,
        val timeModified: Long = 0L
    ) {
        constructor(nbt: NbtCompound) : this(
            name = nbt.getString("Name"),
            author = nbt.getString("Author"),
            description = nbt.getString("Description"),
            timeCreated = nbt.getLong("TimeCreated"),
            timeModified = nbt.getLong("TimeModified")
        )

        fun toNbt(): NbtCompound = NbtCompound().apply {
            putString("Name", name)
            putString("Author", author)
            putString("Description", description)
            putLong("TimeCreated", timeCreated)
            putLong("TimeModified", timeModified)
        }
    }

    data class Region(
        val position: Vec3 = Vec3(),
        val size: Vec3 = Vec3(),
        val blockStatePalette: List<BlockStateEntry> = emptyList(),
        var blockStates: LongArray = longArrayOf(),
        val tileEntities: MutableList<NbtCompound> = mutableListOf(),
        val entities: MutableList<NbtCompound> = mutableListOf()
    ) {
        constructor(nbt: NbtCompound) : this(
            position = Vec3(nbt.getCompound("Position")),
            size = Vec3(nbt.getCompound("Size")),
            blockStatePalette = nbt.getList("BlockStatePalette", 10).map { BlockStateEntry(it as NbtCompound) },
            blockStates = nbt.getLongArray("BlockStates"),
            tileEntities = nbt.getList("TileEntities", 10).mapTo(mutableListOf()) { it as NbtCompound },
            entities = nbt.getList("Entities", 10).mapTo(mutableListOf()) { it as NbtCompound }
        )

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

        /**
         * Decodes the bit-packed blockStates into an IntArray of palette indices.
         */
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

        /**
         * Helper to get a block palette index at (x, y, z).
         * Returns -1 if out of bounds.
         */
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

        /**
         * Helper to set a block palette index at (x, y, z).
         * Automatically updates bit-packed blockStates.
         * Throws if coords out of bounds or blockIndex out of palette range.
         */
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
                mutableStates[nextIndex] = clearedNext or ((blockIndex.toLong() shr (bitsPerBlock - overlapBits)) and ((1L shl overlapBits) - 1))
            }

            blockStates = mutableStates.toLongArray()
        }

        /**
         * Normalizes coordinates considering possible negative region dimensions.
         */
        private fun normalizeCoords(x: Int, y: Int, z: Int): Triple<Int, Int, Int> {
            val nx = if (size.x < 0) size.x + x else x
            val ny = if (size.y < 0) size.y + y else y
            val nz = if (size.z < 0) size.z + z else z
            return Triple(nx, ny, nz)
        }

        /**
         * Returns a sequence of all blocks with their coordinates and palette indices.
         */
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

        /**
         * Helper to find a tile entity at given block coordinates.
         */
        fun findTileEntity(x: Int, y: Int, z: Int): NbtCompound? {
            return tileEntities.firstOrNull {
                it.getInt("x") == x && it.getInt("y") == y && it.getInt("z") == z
            }
        }

        /**
         * Adds or replaces a tile entity at given position.
         */
        fun setTileEntity(tileEntityNbt: NbtCompound) {
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

        /**
         * Removes a tile entity at given block coordinates, returns true if removed.
         */
        fun removeTileEntity(x: Int, y: Int, z: Int): Boolean {
            return tileEntities.removeIf {
                it.getInt("x") == x && it.getInt("y") == y && it.getInt("z") == z
            }
        }

        /**
         * Returns a sequence of all tile entities.
         */
        fun tileEntitiesSequence() = tileEntities.asSequence()

        /**
         * Returns a sequence of all entities.
         */
        fun entitiesSequence() = entities.asSequence()

        fun toNbt(): NbtCompound = NbtCompound().apply {
            put("Position", position.toNbt())
            put("Size", this@Region.size.toNbt())
            val paletteList = NbtList()
            blockStatePalette.forEach { paletteList.add(it.toNbt()) }
            put("BlockStatePalette", paletteList)
            putLongArray("BlockStates", blockStates)
            val tileList = NbtList()
            tileEntities.forEach { tileList.add(it) }
            put("TileEntities", tileList)
            val entityList = NbtList()
            entities.forEach { entityList.add(it) }
            put("Entities", entityList)
        }

        data class BlockStateEntry(
            val name: String = "",
            val properties: NbtCompound = NbtCompound()
        ) {
            constructor(nbt: NbtCompound) : this(
                name = nbt.getString("Name"),
                properties = nbt.getCompound("Properties")
            )

            fun toNbt(): NbtCompound = NbtCompound().apply {
                putString("Name", name)
                put("Properties", properties)
            }
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

            return true
        }

        override fun hashCode(): Int {
            var result = position.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + blockStatePalette.hashCode()
            result = 31 * result + blockStates.contentHashCode()
            result = 31 * result + tileEntities.hashCode()
            result = 31 * result + entities.hashCode()
            return result
        }
    }

    data class Vec3(val x: Int = 0, val y: Int = 0, val z: Int = 0) {
        constructor(nbt: NbtCompound) : this(
            x = nbt.getInt("x"),
            y = nbt.getInt("y"),
            z = nbt.getInt("z")
        )

        fun toNbt(): NbtCompound = NbtCompound().apply {
            putInt("x", x)
            putInt("y", y)
            putInt("z", z)
        }
    }
}
