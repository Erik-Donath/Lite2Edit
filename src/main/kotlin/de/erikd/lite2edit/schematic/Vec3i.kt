package de.erikd.lite2edit.schematic

data class Vec3i(val x: Int, val y: Int, val z: Int) {
    companion object {
        val ZERO = Vec3i(0, 0, 0)
    }
}