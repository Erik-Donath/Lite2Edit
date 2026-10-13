package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.Block
import de.erikd.lite2edit.schematic.BlockState
import de.erikd.lite2edit.schematic.Schematic
import de.erikd.lite2edit.schematic.SchematicBuilder
import de.erikd.lite2edit.schematic.Vec3i
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BlockStatePackingTest {
    @Test
    fun `round trips values across packed long boundaries`() {
        val cases = listOf(
            2 to intArrayOf(0, 1, 0, 1, 1),
            3 to IntArray(65) { it % 3 },
            17 to IntArray(101) { (it * 7) % 17 },
            257 to IntArray(129) { (it * 31) % 257 }
        )

        for ((paletteSize, expected) in cases) {
            val packed = BlockStatePacking.encode(expected, paletteSize)
            assertContentEquals(expected, BlockStatePacking.decode(packed, paletteSize, expected.size))
        }
    }

    @Test
    fun `truncated packed data leaves missing states as air`() {
        val expected = IntArray(30) { it % 5 }
        val packed = BlockStatePacking.encode(expected, paletteSize = 5).copyOf(1)
        val decoded = BlockStatePacking.decode(packed, paletteSize = 5, blockCount = expected.size)
        val completeBlockCount = Long.SIZE_BITS / 3

        assertContentEquals(
            expected.copyOfRange(0, completeBlockCount),
            decoded.copyOfRange(0, completeBlockCount)
        )
        assertEquals(expected[completeBlockCount] and 1, decoded[completeBlockCount])
        assertContentEquals(
            IntArray(expected.size - completeBlockCount - 1),
            decoded.copyOfRange(completeBlockCount + 1, decoded.size)
        )
    }

    @Test
    fun `block count rejects invalid dimensions and overflow`() {
        assertEquals(24, BlockStatePacking.blockCount(Vec3i(2, 3, 4)))
        assertFailsWith<IllegalArgumentException> {
            BlockStatePacking.blockCount(Vec3i(0, 3, 4))
        }
        assertFailsWith<IllegalArgumentException> {
            BlockStatePacking.blockCount(Vec3i(Int.MAX_VALUE, 2, 1))
        }
    }

    @Test
    fun `schematic size rejects integer overflow`() {
        val schematic = Schematic(min = Vec3i.MIN, max = Vec3i.MAX)

        assertFailsWith<IllegalArgumentException> { schematic.size }
    }

    @Test
    fun `region writer and reader preserve block positions and states`() {
        val blocks = listOf(
            Block(Vec3i(-2, 4, 1), BlockState("minecraft:stone")),
            Block(Vec3i(1, 4, 2), BlockState("minecraft:oak_log", mapOf("axis" to "x")))
        )
        val schematic = Schematic(
            blocks = blocks,
            min = Vec3i(-2, 4, 1),
            max = Vec3i(1, 4, 2)
        )
        val result = SchematicBuilder().also {
            RegionReader.parse(it, RegionWriter.parse(schematic))
        }.build()

        assertEquals(
            blocks.associate { it.pos to it.state },
            result.blocks.associate { it.pos to it.state }
        )
    }

    @Test
    fun `encoding rejects IDs outside the palette`() {
        assertFailsWith<IllegalArgumentException> {
            BlockStatePacking.encode(intArrayOf(0, 2), paletteSize = 2)
        }
    }
}
