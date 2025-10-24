package de.erikd.lite2edit

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList

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


data class Schematic(
    var minecraftDataVersion: Int = 0,
    var version: Int = 0,
    var metadata: Metadata = Metadata(),
    var regions: MutableMap<String, Region> = mutableMapOf()
) {
    constructor(nbt: NbtCompound) : this() {
        minecraftDataVersion = nbt.getInt("MinecraftDataVersion")
        version = nbt.getInt("Version")
        metadata = Metadata(nbt.getCompound("Metadata"))
        regions = mutableMapOf<String, Region>().apply {
            val regCompound = nbt.getCompound("Regions")
            for (regionName in regCompound.keys) {
                put(regionName, Region(regCompound.getCompound(regionName)))
            }
        }
    }

    fun toNbt(): NbtCompound {
        val nbt = NbtCompound()
        nbt.putInt("MinecraftDataVersion", minecraftDataVersion)
        nbt.putInt("Version", version)
        nbt.put("Metadata", metadata.toNbt())
        val regCompound = NbtCompound()
        regions.forEach { (name, region) ->
            regCompound.put(name, region.toNbt())
        }
        nbt.put("Regions", regCompound)
        return nbt
    }
}

data class Metadata(
    var name: String = "",
    var author: String = "",
    var description: String = "",
    var timeCreated: Long = 0,
    var timeModified: Long = 0
) {
    constructor(nbt: NbtCompound): this(
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
    var position: Vec3 = Vec3(),
    var size: Vec3 = Vec3(),
    var blockStatePalette: List<BlockStateEntry> = listOf(),
    var blockStates: LongArray = longArrayOf(),
    var tileEntities: List<NbtCompound> = listOf(),
    var entities: List<NbtCompound> = listOf()
) {
    constructor(nbt: NbtCompound): this(
        position = Vec3(nbt.getCompound("Position")),
        size = Vec3(nbt.getCompound("Size")),
        blockStatePalette = nbt.getList("BlockStatePalette", 10).map { BlockStateEntry(it as NbtCompound) },
        blockStates = nbt.getLongArray("BlockStates"),
        tileEntities = (nbt.getList("TileEntities", 10).map { it as NbtCompound }),
        entities = (nbt.getList("Entities", 10).map { it as NbtCompound })
    )

    fun toNbt(): NbtCompound {
        val nbt = NbtCompound()
        nbt.put("Position", position.toNbt())
        nbt.put("Size", size.toNbt())
        val paletteList = NbtList()
        blockStatePalette.forEach { paletteList.add(it.toNbt()) }
        nbt.put("BlockStatePalette", paletteList)
        nbt.putLongArray("BlockStates", blockStates)
        val tileList = NbtList()
        tileEntities.forEach { tileList.add(it) }
        nbt.put("TileEntities", tileList)
        val entityList = NbtList()
        entities.forEach { entityList.add(it) }
        nbt.put("Entities", entityList)
        return nbt
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Region) return false
        return position == other.position &&
                size == other.size &&
                blockStatePalette == other.blockStatePalette &&
                blockStates.contentEquals(other.blockStates) &&
                tileEntities == other.tileEntities &&
                entities == other.entities
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

data class Vec3(var x: Int = 0, var y: Int = 0, var z: Int = 0) {
    constructor(nbt: NbtCompound): this(
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

data class BlockStateEntry(
    var name: String = "",
    var properties: NbtCompound = NbtCompound()
) {
    constructor(nbt: NbtCompound): this(
        name = nbt.getString("Name"),
        properties = nbt.getCompound("Properties")
    )
    fun toNbt(): NbtCompound = NbtCompound().apply {
        putString("Name", name)
        put("Properties", properties)
    }
}
