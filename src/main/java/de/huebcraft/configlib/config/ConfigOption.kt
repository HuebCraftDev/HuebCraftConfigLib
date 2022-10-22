package de.huebcraft.configlib.config

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.cast

/**
 * Delegate for config values.
 */
open class ConfigOption<T : Any>(
    protected val type: KClass<*>,
    protected var value: T,
    internal val key: String,
    private val top: ConfigObject
) {
    open operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    open operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        top.schedulePersist()
    }

    open fun set(any: Any?) {
        if (type.isInstance(any)) {
            @Suppress("UNCHECKED_CAST")
            value = type.cast(any) as T
        }
    }

    open fun get(): Any {
        return value
    }

    fun getKClass(): KClass<*> {
        return type
    }

    internal var wasLoadedFromFile = true
}