package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
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

        val offset = region.offset

        // Build a WorldEdit palette using BlockHelper
        val palette = region.blockStatePalette.map { entry ->
            BlockHelper.parseBlockState(entry.name, entry.properties)
        }

        // TODO: handle empty palette gracefully

        val clipboardRegion = CuboidRegion(
            BlockVector3.ZERO,
            BlockVector3.at(sizeX - 1, sizeY - 1, sizeZ - 1)
        )
        val clipboard = BlockArrayClipboard(clipboardRegion)
        clipboard.origin = BlockVector3.at(offset.x, offset.y, offset.z)

        var blocksSet = 0

        // Use forEachBlock helper for safe iteration
        region.blocksWithCoordinates().forEach { (x, y, z, paletteIndex) ->
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
}
