package de.huebcraft.configlib.util

import com.google.gson.JsonPrimitive
import de.huebcraft.configlib.Main

fun <E, L : MutableList<E>> L.toImmutable(): List<E> {
    return List(size) { this[it] }
}

fun JsonPrimitive.getValue(): Any? {
    return try {
        val valueField = this.javaClass.getDeclaredField("value")

        valueField.isAccessible = true
        valueField[this]
    } catch (e: Exception) {
        Main.LOGGER.error("Exception while unwrapping GSON primitive", e)
        null
    }
}