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

        return convertRegions(schematic.regions)
    }

    private fun convertRegions(regions: Map<String, LitematicaSchematic.Region>): Clipboard {
        if (regions.size == 1) {
            val region = regions.values.first()
            val sizeX = abs(region.size.x)
            val sizeY = abs(region.size.y)
            val sizeZ = abs(region.size.z)
            val offset = region.offset

            val clipboardRegion = CuboidRegion(
                BlockVector3.ZERO,
                BlockVector3.at(sizeX - 1, sizeY - 1, sizeZ - 1)
            )
            val clipboard = BlockArrayClipboard(clipboardRegion)
            clipboard.origin = BlockVector3.at(offset.x, offset.y, offset.z)

            val blocksSet = convertRegion(region, clipboard, 0, 0, 0)
            logger.info("Converted single Litematica region sized $sizeX x $sizeY x $sizeZ, set $blocksSet blocks")
            return clipboard
        }

        // Multiple regions: compute global bounding box
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE

        regions.values.forEach { region ->
            val offset = region.offset
            val sizeX = region.size.x
            val sizeY = region.size.y
            val sizeZ = region.size.z

            val regionMinX = offset.x
            val regionMinY = offset.y
            val regionMinZ = offset.z
            val regionMaxX = offset.x + (if (sizeX >= 0) sizeX - 1 else sizeX + 1)
            val regionMaxY = offset.y + (if (sizeY >= 0) sizeY - 1 else sizeY + 1)
            val regionMaxZ = offset.z + (if (sizeZ >= 0) sizeZ - 1 else sizeZ + 1)

            minX = minOf(minX, regionMinX)
            minY = minOf(minY, regionMinY)
            minZ = minOf(minZ, regionMinZ)
            maxX = maxOf(maxX, regionMaxX)
            maxY = maxOf(maxY, regionMaxY)
            maxZ = maxOf(maxZ, regionMaxZ)
        }

        val totalWidth = maxX - minX + 1
        val totalHeight = maxY - minY + 1
        val totalLength = maxZ - minZ + 1

        val clipboardRegion = CuboidRegion(
            BlockVector3.ZERO,
            BlockVector3.at(totalWidth - 1, totalHeight - 1, totalLength - 1)
        )
        val clipboard = BlockArrayClipboard(clipboardRegion)
        clipboard.origin = BlockVector3.at(minX, minY, minZ)

        var blocksSet = 0
        regions.values.forEach { region ->
            val offset = region.offset
            val relX = offset.x - minX
            val relY = offset.y - minY
            val relZ = offset.z - minZ

            blocksSet += convertRegion(region, clipboard, relX, relY, relZ)
        }
        logger.info("Converted multiple Litematica regions sized $totalWidth x $totalHeight x $totalLength, set $blocksSet blocks")
        return clipboard
    }

    private fun convertRegion(
        region: LitematicaSchematic.Region,
        clipboard: BlockArrayClipboard,
        offsetX: Int,
        offsetY: Int,
        offsetZ: Int
    ): Int {
        // TODO: Consider caching the palette conversion if regions are large or accessed multiple times
        val palette = region.blockStatePalette.map { entry ->
            BlockHelper.parseBlockState(entry.name, entry.properties)
        }

        var blocksSet = 0
        region.blocksWithCoordinates().forEach { (x, y, z, paletteIndex) ->
            if (paletteIndex in palette.indices) {
                val blockState = palette[paletteIndex]
                if (blockState != null) {
                    try {
                        clipboard.setBlock(BlockVector3.at(offsetX + x, offsetY + y, offsetZ + z), blockState)
                        blocksSet++
                    } catch (e: Exception) {
                        // TODO: Evaluate whether to continue after errors or abort
                        // Log detailed error for better tracing
                        logger.error(
                            "Failed to set block at (${offsetX + x}, ${offsetY + y}, ${offsetZ + z}): ${blockState.asString}",
                            e
                        )
                    }
                } else {
                    // TODO: Decide how to handle null block states in the palette gracefully
                    logger.warn("Null block state for palette index $paletteIndex")
                }
            } else {
                // TODO: Validate palette index consistency during schematic load before conversion
                logger.warn("Invalid palette index: $paletteIndex (palette size: ${palette.size})")
            }
        }

        // TODO: Extend to convert tile entities and entities into clipboard or related storage

        return blocksSet
    }
}
