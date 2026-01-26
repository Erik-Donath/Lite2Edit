@file:Suppress("UnusedSymbol")
package de.erikd.lite2edit.schematic

import kotlin.math.abs

data class Vec3i(val x: Int, val y: Int, val z: Int) {
    operator fun plus(other: Vec3i) = Vec3i(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3i) = Vec3i(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Int) = Vec3i(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Int) = Vec3i(x / scalar, y / scalar, z / scalar)
    operator fun unaryMinus() = Vec3i(-x, -y, -z)

    fun abs() = Vec3i(abs(x), abs(y), abs(z))

    companion object {
        val ZERO = Vec3i(0, 0, 0)
        val ONE = Vec3i(1, 1, 1)
        val MIN = Vec3i(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
        val MAX = Vec3i(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
    }
}