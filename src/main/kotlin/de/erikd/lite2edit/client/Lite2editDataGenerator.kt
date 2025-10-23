package de.erikd.lite2edit.client

import com.sk89q.worldedit.WorldEdit
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class Lite2editDataGenerator : DataGeneratorEntrypoint {

    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()
        val worldEdit = WorldEdit.getInstance()
        println("WorldEdit wurde gefunden: $worldEdit das pack $pack")
    }
}
