package de.erikd.lite2edit

import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import net.fabricmc.api.ModInitializer

class Lite2edit : ModInitializer {
    override fun onInitialize() {
        ClipboardFormats.registerClipboardFormat(LitematicSchematicFormat())
    }
}
