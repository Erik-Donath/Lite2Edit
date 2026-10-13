package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.schematic.Vec3i

internal object BlockStatePacking {
    fun blockCount(size: Vec3i): Int {
        require(size.x > 0 && size.y > 0 && size.z > 0) { "Region dimensions must be positive: $size" }

        val xy = size.x.toLong() * size.y
        require(xy <= Int.MAX_VALUE.toLong() / size.z) {
            "Region dimensions exceed the supported block count: $size"
        }
        return (xy * size.z).toInt()
    }

    fun decode(packed: LongArray, paletteSize: Int, blockCount: Int): IntArray {
        require(blockCount >= 0) { "Block count cannot be negative: $blockCount" }
        val result = IntArray(blockCount)
        if (paletteSize <= 0 || packed.isEmpty() || blockCount == 0) return result

        val bitsPerBlock = bitsPerBlock(paletteSize)
        val mask = (1L shl bitsPerBlock) - 1

        for (blockIndex in result.indices) {
            val bitOffset = blockIndex.toLong() * bitsPerBlock
            val longIndex = (bitOffset ushr 6).toInt()
            if (longIndex >= packed.size) break

            val bitShift = (bitOffset and 63).toInt()
            var value = packed[longIndex] ushr bitShift
            if (bitShift + bitsPerBlock > Long.SIZE_BITS && longIndex + 1 < packed.size) {
                value = value or (packed[longIndex + 1] shl (Long.SIZE_BITS - bitShift))
            }
            result[blockIndex] = (value and mask).toInt()
        }

        return result
    }

    fun encode(blockIds: IntArray, paletteSize: Int): LongArray {
        if (blockIds.isEmpty()) return LongArray(0)
        require(paletteSize > 0) { "Palette cannot be empty" }

        val bitsPerBlock = bitsPerBlock(paletteSize)
        val totalBits = blockIds.size.toLong() * bitsPerBlock
        val longCount = (totalBits + Long.SIZE_BITS - 1) / Long.SIZE_BITS
        require(longCount <= Int.MAX_VALUE) { "Packed block data is too large: $longCount longs" }

        val result = LongArray(longCount.toInt())
        val mask = (1L shl bitsPerBlock) - 1
        for ((blockIndex, blockId) in blockIds.withIndex()) {
            require(blockId in 0 until paletteSize) {
                "Block state ID $blockId is outside palette size $paletteSize"
            }

            val bitOffset = blockIndex.toLong() * bitsPerBlock
            val longIndex = (bitOffset ushr 6).toInt()
            val bitShift = (bitOffset and 63).toInt()
            val value = blockId.toLong() and mask

            result[longIndex] = result[longIndex] or (value shl bitShift)
            if (bitShift + bitsPerBlock > Long.SIZE_BITS) {
                result[longIndex + 1] = result[longIndex + 1] or
                    (value ushr (Long.SIZE_BITS - bitShift))
            }
        }

        return result
    }

    private fun bitsPerBlock(paletteSize: Int): Int =
        maxOf(2, Int.SIZE_BITS - Integer.numberOfLeadingZeros(paletteSize - 1))
}
