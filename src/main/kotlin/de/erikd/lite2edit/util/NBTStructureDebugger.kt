package de.erikd.lite2edit.litematica

import de.erikd.lite2edit.util.value
import net.kyori.adventure.nbt.*
import org.slf4j.LoggerFactory

/**
 * Walks an NBT tree and emits a compact structural description.
 *
 * • Every compound key is printed with its tag type.
 * • Lists/arrays print their size.
 * • For a list whose elements are compounds we also print the field names
 *   of the **first** element (that tells you the shape without spamming
 *   the whole file).
 *
 * The output is indented so you can see the nesting levels.
 */
object NbtStructureDebugger {
    private val logger = LoggerFactory.getLogger(LitematicaReader::class.java)

    /** Entry point – call once with the root tag. */
    fun dump(root: CompoundBinaryTag) {
        logger.info("[NBT STRUCTURE] Root compound (${root.keySet().size} top‑level keys)")
        walkCompound(root, indent = "")
    }

    /** Recursively walk a compound tag. */
    private fun walkCompound(compound: CompoundBinaryTag, indent: String) {
        val keys = compound.keySet()
        for ((i, key) in keys.withIndex()) {
            val isLast = i == keys.size - 1
            val childIndent = indent + if (isLast) "    " else "│   "

            val tag = compound.get(key) ?: continue
            val typeName = tag.javaClass.simpleName.removeSuffix("BinaryTag")
            val prefix = indent + if (isLast) "└─ " else "├─ "



            when (tag) {
                is CompoundBinaryTag -> {
                    logger.info("$prefix$key ($typeName) – ${tag.keySet().size} sub‑keys")
                    walkCompound(tag, childIndent)
                }
                is ListBinaryTag -> {
                    val elemType = tag.elementType()
                    val size = tag.size()
                    val extra = if (elemType == BinaryTagTypes.COMPOUND && size > 0) {
                        // Show the field names of the first element only
                        val first = tag.getCompound(0)
                        " – first keys: ${first.keySet().joinToString()}"
                    } else ""
                    logger.info("$prefix$key (LIST<$elemType>) – $size elems$extra")
                    // No deeper recursion into list elements – they could be huge.
                }
                else -> {
                    logger.info("$prefix$key ($typeName) - ${tag.value()}")
                }
            }
        }
    }
}