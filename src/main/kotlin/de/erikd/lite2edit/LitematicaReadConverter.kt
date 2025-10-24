package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockTypes
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import org.slf4j.LoggerFactory
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.max

object LitematicaReadConverter {
    private val logger = LoggerFactory.getLogger(LitematicaReadConverter::class.java)

    fun read(inputStream: InputStream): Clipboard {
        val schematic = Schematic(
            NbtIo.readCompressed(inputStream, NbtSizeTracker.ofUnlimitedBytes())
        )
        if (schematic.regions.isEmpty()) throw IllegalStateException("No regions found in schematic")
        // Use first region (TODO: handle multiple regions)
        val (regionName, region) = schematic.regions.entries.first()

        return convertRegion(region)
    }

    private fun convertRegion(region: Region): Clipboard {
        // Dimensions
        val sizeX = region.size.x
        val sizeY = region.size.y
        val sizeZ = region.size.z

        // Offset calculation per litematic format
        val offsetX = region.position.x + if (sizeX < 0) sizeX + 1 else 0
        val offsetY = region.position.y + if (sizeY < 0) sizeY + 1 else 0
        val offsetZ = region.position.z + if (sizeZ < 0) sizeZ + 1 else 0

        // Build palette (use WorldEdit block types)
        val palette = region.blockStatePalette.map { entry ->
            try {
                val blockType = BlockTypes.get(entry.name)
                if (blockType == null) {
                    logger.warn("Unknown block type: ${entry.name}")
                    null
                } else {
                    parseBlockState(blockType, entry.properties)
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse block: ${entry.name}", e)
                null
            }
        }

        // Decode block states (bit-packed)
        val blockStates = region.blockStates
        val bitsPerBlock = max(2, 32 - Integer.numberOfLeadingZeros(palette.size - 1))
        val numBlocks = abs(sizeX * sizeY * sizeZ)
        val blocks = decodeBlocks(blockStates, bitsPerBlock, numBlocks)

        // Set up WorldEdit clipboard
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
        logger.info("Converted Litematica region: ${width}x${height}x${length}")
        return clipboard
    }

    // Helper to parse blockstate from entry
    private fun parseBlockState(
        blockType: com.sk89q.worldedit.world.block.BlockType,
        properties: net.minecraft.nbt.NbtCompound?
    ): BlockState {
        var state = blockType.defaultState
        if (properties == null || properties.isEmpty) return state

        for (propName in properties.keys) {
            val propValue = properties.getString(propName)
            try {
                val props = blockType.properties
                val property = props.find { it.name.equals(propName, ignoreCase = true) }
                if (property != null) {
                    val values = property.values
                    val matchingValue = values.find { it.toString().equals(propValue, ignoreCase = true) }
                    if (matchingValue != null) {
                        @Suppress("UNCHECKED_CAST")
                        state = state.with(
                            property as com.sk89q.worldedit.registry.state.Property<Any>,
                            matchingValue
                        )
                    }
                }
            } catch (e: Exception) {
                logger.debug("Failed to set property $propName=$propValue: ${e.message}")
            }
        }
        return state
    }

    // Bit-packed block indices decode
    private fun decodeBlocks(blockStates: LongArray, bitsPerBlock: Int, numBlocks: Int): IntArray {
        val result = IntArray(numBlocks)
        var longIndex = 0
        var bitIndex = 0
        val mask = (1L shl bitsPerBlock) - 1L

        for (i in 0 until numBlocks) {
            val bitOffset = i * bitsPerBlock
            longIndex = bitOffset / 64
            bitIndex = bitOffset % 64
            val current = blockStates.getOrNull(longIndex) ?: 0L
            val value: Long = if (bitIndex + bitsPerBlock <= 64) {
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
