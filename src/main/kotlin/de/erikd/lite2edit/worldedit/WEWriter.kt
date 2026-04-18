package de.erikd.lite2edit.worldedit

import com.sk89q.worldedit.entity.BaseEntity
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.util.concurrency.LazyReference
import org.enginehub.linbus.tree.LinCompoundTag
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.registry.state.Property
import com.sk89q.worldedit.util.Location
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockTypes
import com.sk89q.worldedit.world.entity.EntityTypes
import de.erikd.lite2edit.schematic.*
import de.erikd.lite2edit.util.toLinbus
import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.DoubleBinaryTag
import org.slf4j.LoggerFactory

object WEWriter {
    private val LOGGER = LoggerFactory.getLogger(WEWriter::class.java)
    fun write(schematic: Schematic): Clipboard {
        val region = CuboidRegion(
            schematic.min.toBlockVector3(),
            schematic.max.toBlockVector3()
        )

        val clipboard = BlockArrayClipboard(region)
        clipboard.origin = schematic.min.toBlockVector3()

        for (block in schematic.blocks) {
            val state = parseBlockState(block.state.name, block.state.properties) ?: continue
            clipboard.setBlock(
                block.pos.toBlockVector3(),
                state.toBaseBlock(block.state.nbt?.toLinbus())
            )
        }

        for (entity in schematic.entities) {
            val nbt = entity.nbt

            // Litematica stores entity position as a list of 3 doubles under "Pos"
            val posList = nbt.getList("Pos", BinaryTagTypes.DOUBLE)
            if (posList.size() < 3) continue

            val x = (posList[0] as DoubleBinaryTag).value()
            val y = (posList[1] as DoubleBinaryTag).value()
            val z = (posList[2] as DoubleBinaryTag).value()

            val typeId = nbt.getString("id").takeIf { it.isNotEmpty() } ?: continue
            val entityType = EntityTypes.get(typeId)
            if (entityType == null) {
                LOGGER.warn("Lite2Edit: unknown entity type '{}' - skipping entity", typeId)
                continue
            }

            val baseEntity = BaseEntity(entityType, LazyReference.from<LinCompoundTag> { nbt.toLinbus() })
            clipboard.createEntity(Location(clipboard, x, y, z), baseEntity)
        }

        return clipboard
    }

    private fun parseBlockState(name: String, properties: Map<String, String>): BlockState? {
        val blockType = BlockTypes.get(name) ?: run {
            LOGGER.warn("Lite2Edit: unknown block type '{}' — skipping block", name)
            return null
        }
        var state = blockType.defaultState

        for ((propName, propValue) in properties) {
            val property = blockType.properties.find {
                it.name.equals(propName, ignoreCase = true)
            } ?: continue

            val candidate = property.values.find {
                it.toString().equals(propValue, ignoreCase = true)
            } ?: continue

            @Suppress("UNCHECKED_CAST")
            state = state.with(property as Property<Any>, candidate)
        }
        return state
    }
}