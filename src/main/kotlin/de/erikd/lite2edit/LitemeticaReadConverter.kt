
package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockTypes
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import org.slf4j.LoggerFactory
import java.io.InputStream
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

object LitemeticaReadConverter {

    private val logger = LoggerFactory.getLogger(LitemeticaReadConverter::class.java)

    fun read(inputStream: InputStream): Clipboard {
        val root = NbtIo.readCompressed(inputStream, NbtSizeTracker.ofUnlimitedBytes())
        val regions = root.getCompound("Regions")

        // Use first region (TODO: handle multiple regions)
        val regionName = regions.keys.first()
        val region = regions.getCompound(regionName)

        return convertRegion(region)
    }

    private fun convertRegion(region: NbtCompound): Clipboard {
        // Get dimensions
        val size = region.getCompound("Size")
        val sizeX = size.getInt("x")
        val sizeY = size.getInt("y")
        val sizeZ = size.getInt("z")

        // Get offset
        val position = region.getCompound("Position")
        val offsetX = position.getInt("x") + if (sizeX < 0) sizeX + 1 else 0
        val offsetY = position.getInt("y") + if (sizeY < 0) sizeY + 1 else 0
        val offsetZ = position.getInt("z") + if (sizeZ < 0) sizeZ + 1 else 0

        // Build palette
        val paletteList = region.getList("BlockStatePalette", 10)
        val palette = buildPalette(paletteList)

        // Decode block states
        val blockStates = region.getLongArray("BlockStates")
        val bitsPerBlock = max(2, 32 - Integer.numberOfLeadingZeros(palette.size - 1))
        val numBlocks = abs(sizeX * sizeY * sizeZ)
        val blocks = decodeBlocks(blockStates, bitsPerBlock, numBlocks)

        // Create clipboard
        val width = abs(sizeX)
        val height = abs(sizeY)
        val length = abs(sizeZ)

        val clipboardRegion = CuboidRegion(
            BlockVector3.ZERO,
            BlockVector3.at(width - 1, height - 1, length - 1)
        )
        val clipboard = BlockArrayClipboard(clipboardRegion)
        clipboard.origin = BlockVector3.at(offsetX, offsetY, offsetZ)

        // Fill clipboard with blocks
        fillClipboard(clipboard, palette, blocks, width, height, length)

        // TODO: Handle tile entities
        // val tileEntities = region.getList("TileEntities", 10)

        // TODO: Handle entities
        // val entities = region.getList("Entities", 10)

        logger.info("Converted Litematica region: ${width}x${height}x${length}")
        return clipboard
    }

    private fun buildPalette(paletteList: net.minecraft.nbt.NbtList): List<BlockState?> {
        val palette = mutableListOf<BlockState?>()

        for (i in 0 until paletteList.size) {
            val blockTag = paletteList.getCompound(i)
            val blockName = blockTag.getString("Name")

            try {
                // Parse block type from name (e.g., "minecraft:stone")
                val blockType = BlockTypes.get(blockName)

                if (blockType == null) {
                    logger.warn("Unknown block type: $blockName")
                    palette.add(null)
                    continue
                }

                // Get default state
                var blockState = blockType.defaultState

                // Apply properties if they exist
                val properties = blockTag.getCompound("Properties")
                if (!properties.isEmpty) {
                    for (propertyName in properties.keys) {
                        val propertyValue = properties.getString(propertyName)

                        try {
                            blockState = blockState.with(
                                blockType.getProperty(propertyName),
                                propertyValue
                            )
                        } catch (e: Exception) {
                            logger.debug("Failed to set property $propertyName=$propertyValue for $blockName")
                        }
                    }
                }

                palette.add(blockState)
            } catch (e: Exception) {
                logger.warn("Failed to parse block: $blockName", e)
                palette.add(null)
            }
        }

        return palette
    }

    private fun decodeBlocks(blockStates: LongArray, bitsPerBlock: Int, numBlocks: Int): IntArray {
        val result = IntArray(numBlocks)
        var index = 0
        var bits = 0L
        var bitCount = 0
        val mask = (1L shl bitsPerBlock) - 1

        for (num in blockStates) {
            var current = num
            var available = bitCount + 64

            if (bitCount > 0) {
                val needed = bitsPerBlock - bitCount
                bits = bits or ((current and ((1L shl needed) - 1)) shl bitCount)
                current = current ushr needed
                available -= bitsPerBlock
                if (index < numBlocks) result[index++] = bits.toInt()
            }

            while (available >= bitsPerBlock && index < numBlocks) {
                result[index++] = (current and mask).toInt()
                current = current ushr bitsPerBlock
                available -= bitsPerBlock
            }

            bits = current
            bitCount = available
        }

        return result
    }

    private fun fillClipboard(
        clipboard: Clipboard,
        palette: List<BlockState?>,
        blocks: IntArray,
        width: Int,
        height: Int,
        length: Int
    ) {
        var index = 0
        var blocksSet = 0

        for (y in 0 until height) {
            for (z in 0 until length) {
                for (x in 0 until width) {
                    if (index >= blocks.size) {
                        logger.error("Block index out of bounds: $index >= ${blocks.size}")
                        return
                    }

                    val paletteIndex = blocks[index++]

                    if (paletteIndex >= 0 && paletteIndex < palette.size) {
                        val blockState = palette[paletteIndex]

                        if (blockState != null) {
                            try {
                                clipboard.setBlock(BlockVector3.at(x, y, z), blockState)
                                blocksSet++
                            } catch (e: Exception) {
                                logger.error("Failed to set block at ($x, $y, $z): ${blockState.asString}", e)
                            }
                        }
                    } else {
                        logger.warn("Invalid palette index: $paletteIndex (palette size: ${palette.size})")
                    }
                }
            }
        }

        logger.info("Set $blocksSet blocks in clipboard")
    }
}