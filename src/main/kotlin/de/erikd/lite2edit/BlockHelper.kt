package de.erikd.lite2edit

import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockTypes
import net.kyori.adventure.nbt.CompoundBinaryTag
import org.slf4j.LoggerFactory

object BlockHelper {
    private val logger = LoggerFactory.getLogger(BlockHelper::class.java)

    fun parseBlockState(name: String, properties: CompoundBinaryTag?): BlockState? {
        val blockType = BlockTypes.get(name)
        if (blockType == null) {
            logger.warn("Unknown block type: $name")
            return null
        }

        var state = blockType.defaultState
        if (properties == null || properties.isEmpty()) return state

        for (propName in properties.keySet()) {
            val propValue = properties.getString(propName)
            try {
                val props = blockType.properties
                val property = props.find { it.name.equals(propName, ignoreCase = true) }
                if (property != null) {
                    val values = property.values
                    val matchingValue = values.find { it.toString().equals(propValue, ignoreCase = true) }
                    if (matchingValue != null) {
                        @Suppress("UNCHECKED_CAST")
                        state = state.with(property as com.sk89q.worldedit.registry.state.Property<Any>, matchingValue)
                    }
                }
            } catch (e: Exception) {
                logger.debug("Failed to set property $propName=$propValue: ${e.message}")
            }
        }
        return state
        // TODO Add reverse mapping from BlockState to Litematica palette entry name/properties
    }
}
