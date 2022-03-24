package de.huebcraft.configlib

import com.google.gson.JsonPrimitive

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

val JsonPrimitive.asAny: Any?
    get() {
        return getValue()
    }