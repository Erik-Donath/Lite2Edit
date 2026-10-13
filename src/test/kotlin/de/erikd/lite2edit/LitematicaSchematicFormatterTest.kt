package de.erikd.lite2edit

import de.erikd.lite2edit.litematica.FileHelper
import net.kyori.adventure.nbt.CompoundBinaryTag
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LitematicaSchematicFormatterTest {
    private val formatter = LitematicaSchematicFormatter()

    @Test
    fun `recognizes a large gzip-compressed litematica header`() {
        val payload = ByteArray(2_000) {
            Random(it.toLong()).nextInt(26).plus('a'.code).toByte()
        }.decodeToString()
        val root = CompoundBinaryTag.builder()
            .putInt("Version", 6)
            .put("Metadata", CompoundBinaryTag.empty())
            .putString("Payload", payload)
            .build()
        val output = ByteArrayOutputStream()
        FileHelper.saveLitematic(root, output)

        assertTrue(formatter.isFormat(ByteArrayInputStream(output.toByteArray())))
    }

    @Test
    fun `rejects gzip-compressed non-litematica nbt`() {
        val root = CompoundBinaryTag.builder()
            .putString("Data", "not a Litematica schematic")
            .build()
        val output = ByteArrayOutputStream()
        FileHelper.saveLitematic(root, output)

        assertFalse(formatter.isFormat(ByteArrayInputStream(output.toByteArray())))
    }

    @Test
    fun `recognizes file extensions without locale-dependent case conversion`() {
        assertTrue(formatter.isFormat(java.io.File("schematic.LITEMATIC")))
        assertTrue(formatter.isFormat(java.io.File("schematic.LTC")))
        assertFalse(formatter.isFormat(java.io.File("schematic.schem")))
    }
}
