package de.huebcraft.configlib.config

import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Specialization of [ConfigOption] for collections.
 */
open class CollectionConfigOption<E : Any, U : MutableCollection<E>>(
    private val innerType: KClass<E>, collectionType: KClass<U>, value: U, key: String
) : ConfigOption<U>(collectionType, value, key) {
    override fun set(any: Any?) {
        if (any is MutableCollection<*>) {
            for (element in any) {
                if (!innerType.isInstance(element)) {
                    return
                }
            }

            value.clear()
            value.addAll(type.cast(any))
        }
    }

    fun getElementKClass(): KClass<E> {
        return innerType
    }
}