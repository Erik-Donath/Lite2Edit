@file:Suppress("unused")
package de.erikd.lite2edit.worldedit

import com.sk89q.worldedit.math.BlockVector3
import de.erikd.lite2edit.schematic.Vec3i

internal fun Vec3i.toBlockVector3(): BlockVector3 = BlockVector3.at(x, y, z)

internal fun Vec3i.Companion.fromBlockVector3(vec: BlockVector3): Vec3i = Vec3i(
    x = vec.x(),
    y = vec.y(),
    z = vec.z()
)