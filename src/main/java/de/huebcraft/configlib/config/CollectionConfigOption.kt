package de.huebcraft.configlib.config

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.cast

/**
 * Specialization of [ConfigOption] for collections.
 */
open class CollectionConfigOption<E : Any, U : MutableCollection<E>>(
    private val innerType: KClass<E>,
    value: U,
    key: String,
    private val top: ConfigObject
) : ConfigOption<MutableCollection<E>>(MutableCollection::class, MutableCollectionWrapper(value, top), key, top) {

    override fun getValue(thisRef: Any?, property: KProperty<*>): MutableCollection<E> {
        return value.toMutableList()
    }
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: MutableCollection<E>) {
        this.value.clear()
        this.value.addAll(value)
        top.schedulePersist()
    }

    override fun set(any: Any?) {
        if (any is MutableCollection<*>) {
            for (element in any) {
                if (!innerType.isInstance(element)) {
                    return
                }
            }

            value.clear()
            @Suppress("UNCHECKED_CAST")
            value.addAll(type.cast(any) as U)
        }
    }

    fun getElementKClass(): KClass<E> {
        return innerType
    }
}