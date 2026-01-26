package de.erikd.lite2edit.worldedit

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.registry.state.Property
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockTypes
import de.erikd.lite2edit.schematic.*
import de.erikd.lite2edit.util.toLinbus

class WEWriter {
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

        return clipboard
    }

    private fun parseBlockState(name: String, properties: Map<String, Any?>): BlockState? {
        val blockType = BlockTypes.get(name) ?: return null
        var state = blockType.defaultState

        for ((propName, propValue) in properties) {
            val property = blockType.properties.find {
                it.name.equals(propName, ignoreCase = true)
            } ?: continue

            val value = property.values.find { canidate ->
                canidate.toString().equals(propValue.toString(), ignoreCase = true)
            } ?: continue

            @Suppress("UNCHECKED_CAST")
            state = state.with(property as Property<Any>, value)
        }
        return state
    }
}