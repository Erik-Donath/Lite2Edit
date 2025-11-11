package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.util.Location
import net.kyori.adventure.nbt.BinaryTagTypes
import org.slf4j.LoggerFactory
import java.io.InputStream
import kotlin.math.abs

object ReadConverter {
    private val logger = LoggerFactory.getLogger(ReadConverter::class.java)

    fun read(inputStream: InputStream): Clipboard {
        val root = NBTHelper.loadLitematic(inputStream)

        val schematic = LitematicaSchematic(root)
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

            val (blocksSet, tileEntitiesSet, entitiesSet) = convertRegion(region, clipboard, 0, 0, 0)
            logger.info("Converted single Litematica region sized ${sizeX}x${sizeY}x${sizeZ}, set $blocksSet blocks with $tileEntitiesSet tile entities and $entitiesSet entities")
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

            val regionMaxX = offset.x + if (sizeX >= 0) sizeX - 1 else sizeX + 1
            val regionMaxY = offset.y + if (sizeY >= 0) sizeY - 1 else sizeY + 1
            val regionMaxZ = offset.z + if (sizeZ >= 0) sizeZ - 1 else sizeZ + 1

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

        var totalBlocksSet = 0
        var totalTileEntitiesSet = 0
        var totalEntitiesSet = 0

        regions.values.forEach { region ->
            val offset = region.offset
            val relX = offset.x - minX
            val relY = offset.y - minY
            val relZ = offset.z - minZ

            val (blocksSet, tileEntitiesSet, entitiesSet) = convertRegion(region, clipboard, relX, relY, relZ)
            totalBlocksSet += blocksSet
            totalTileEntitiesSet += tileEntitiesSet
            totalEntitiesSet += entitiesSet
        }

        logger.info("Converted multiple Litematica regions sized ${totalWidth}x${totalHeight}x${totalLength}, set $totalBlocksSet blocks with $totalTileEntitiesSet tile entities and $totalEntitiesSet entities")
        return clipboard
    }

    private fun convertRegion(
        region: LitematicaSchematic.Region,
        clipboard: BlockArrayClipboard,
        offsetX: Int,
        offsetY: Int,
        offsetZ: Int
    ): Triple<Int, Int, Int> {
        val palette = region.blockStatePalette.map { entry ->
            BlockHelper.parseBlockState(entry.name, entry.properties) // entry.properties now CompoundBinaryTag
        }

        // Create tile entity lookup map for fast access
        val tileEntityMap = TileEntityHelper.createTileEntityMap(region.tileEntities)

        var blocksSet = 0
        var tileEntitiesSet = 0
        var entitiesSet = 0

        // Iterate sequence correctly: destructure Quad
        for (quad in region.blocksWithCoordinates()) {
            val x = quad.x
            val y = quad.y
            val z = quad.z
            val paletteIndex = quad.paletteIndex

            if (paletteIndex in palette.indices) {
                val blockState = palette[paletteIndex]
                if (blockState != null) {
                    try {
                        val worldX = offsetX + x
                        val worldY = offsetY + y
                        val worldZ = offsetZ + z
                        val position = BlockVector3.at(worldX, worldY, worldZ)

                        // Check if tile entity exists for this position
                        val tileEntityNbt = tileEntityMap[TileEntityHelper.positionKey(x, y, z)]

                        // Create block with or without tile entity data
                        val baseBlock = TileEntityHelper.createBaseBlock(blockState, tileEntityNbt)
                        clipboard.setBlock(position, baseBlock)
                        blocksSet++
                        if (tileEntityNbt != null) tileEntitiesSet++
                    } catch (e: Exception) {
                        logger.error(
                            "Failed to set block at (${offsetX + x}, ${offsetY + y}, ${offsetZ + z}) ${blockState.asString}",
                            e
                        )
                    }
                } else {
                    logger.warn("Null block state for palette index $paletteIndex")
                }
            } else {
                logger.warn("Invalid palette index $paletteIndex (palette size ${palette.size})")
            }
        }

        for (entityNbt in region.entities) {
            try {
                val base = EntityHelper.convertLitematicaEntity(entityNbt)
                if (base != null) {
                    val posDouble = entityNbt.getList("Pos", BinaryTagTypes.DOUBLE)

                    val (ex, ey, ez) = when {
                        posDouble.size() >= 3 ->
                            Triple(posDouble.getDouble(0), posDouble.getDouble(1), posDouble.getDouble(2))
                        else -> Triple(0.0, 0.0, 0.0)
                    }

                    val worldX = offsetX + ex
                    val worldY = offsetY + ey
                    val worldZ = offsetZ + ez

                    val yaw = if (entityNbt.contains("Yaw")) entityNbt.getFloat("Yaw") else 0f
                    val pitch = if (entityNbt.contains("Pitch")) entityNbt.getFloat("Pitch") else 0f

                    val loc = Location(null, Vector3.at(worldX, worldY, worldZ), yaw, pitch)
                    clipboard.createEntity(loc, base)
                    entitiesSet++
                }
            } catch (e: Exception) {
                logger.error(
                    "Failed to add entity at (${offsetX}, ${offsetY}, ${offsetZ}) with NBT id=${entityNbt.getString("id")}: ${e.message}",
                    e
                )
            }
        }

        return Triple(blocksSet, tileEntitiesSet, entitiesSet)
    }
}
