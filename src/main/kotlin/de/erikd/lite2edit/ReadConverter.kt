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

object ReadConverter {
    private val logger = LoggerFactory.getLogger(ReadConverter::class.java)

    fun read(inputStream: InputStream): Clipboard {
        val schematic = LitematicaSchematic(
            NbtIo.readCompressed(inputStream, NbtSizeTracker.ofUnlimitedBytes())
        )
        if (schematic.regions.isEmpty()) throw IllegalStateException("No regions found in schematic")
        // TODO: handle multiple regions, not just first
        val (_, region) = schematic.regions.entries.first()

        return convertRegion(region)
    }

    private fun convertRegion(region: LitematicaSchematic.Region): Clipboard {
        val sizeX = abs(region.size.x)
        val sizeY = abs(region.size.y)
        val sizeZ = abs(region.size.z)

        // Calculate offset according to region position and negative size
        val offsetX = region.position.x + if (region.size.x < 0) region.size.x + 1 else 0
        val offsetY = region.position.y + if (region.size.y < 0) region.size.y + 1 else 0
        val offsetZ = region.position.z + if (region.size.z < 0) region.size.z + 1 else 0

        // Build a WorldEdit palette of BlockStates from blockStatePalette entries
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

        // TODO: handle empty palette gracefully

        // Create WorldEdit clipboard with dimensions
        val clipboardRegion = CuboidRegion(
            BlockVector3.ZERO,
            BlockVector3.at(sizeX - 1, sizeY - 1, sizeZ - 1)
        )
        val clipboard = BlockArrayClipboard(clipboardRegion)
        clipboard.origin = BlockVector3.at(offsetX, offsetY, offsetZ)

        // Fill clipboard with blocks using Region API for safe access
        var blocksSet = 0
        for ((x, y, z, paletteIndex) in region.blocksWithCoordinates()) {
            if (paletteIndex in palette.indices) {
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
        logger.info("Converted Litematica region: ${sizeX}x${sizeY}x${sizeZ}, set $blocksSet blocks")

        // TODO: convert tile entities and entities when adding this to clipboard or related structure

        return clipboard
    }

    // Helper to parse WorldEdit BlockState from blockType and properties NbtCompound
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
}
