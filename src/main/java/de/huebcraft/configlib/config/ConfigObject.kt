package de.huebcraft.configlib.config

import de.huebcraft.configlib.ConfigFileRegistry
import de.huebcraft.configlib.HuebCraftConfigLib
import de.huebcraft.configlib.util.toImmutable
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible

/**
 * Represents a JSON object.
 * Can be used in collection [ConfigOption]s or as
 * a kotlin singleton object inside another [ConfigObject].
 * @param key JSON key of the object in its parent [ConfigObject].
 */
abstract class ConfigObject(val key: String = "") {
    private var topInternal: ConfigObject? = null

    val top
        get() = if (this.javaClass.annotations.any { it is ConfigFile }) this else topInternal!!

    internal constructor(top: ConfigObject, key: String = "") : this(key) {
        this.topInternal = top
    }

    internal val fileName by lazy {
        val fileAnnotation = this.javaClass.annotations.firstOrNull { it is ConfigFile }
        (fileAnnotation as ConfigFile).fileName
    }

    internal val modId by lazy {
        val fileAnnotation = this.javaClass.annotations.firstOrNull { it is ConfigFile }
        (fileAnnotation as ConfigFile).modId
    }

    @PublishedApi
    internal val optionsMap: HashMap<String, ConfigOption<*>> = HashMap()
    private val objectsMap: HashMap<String, ConfigObject> = HashMap()
    private val optionsList: MutableList<ConfigOption<*>> = mutableListOf()
    private val objectsList: MutableList<ConfigObject> = mutableListOf()


    /**
     * Adds a new plain [ConfigOption] to the [ConfigObject].
     * @param default Default value, will be used if not specified in the config file or config file is not present.
     * @param key JSON key of the [ConfigOption] in the parent object.
     */
    @Suppress("unused")
    protected inline fun <reified T : Any> option(default: T, key: String): ConfigOption<T> {
        val kClass = T::class
        return ConfigOption(kClass, default, key, top)
    }

    /**
     * Adds a new [CollectionConfigOption] to the [ConfigObject].
     *
     * Special case: If the collection element type is a [ConfigObject],
     * these objects will be initialized and stored correctly.
     *
     * **Warning:** There are no warranties for the type of the returned collection from this field.
     * It is usually an instance of [ArrayList], but it may also be of any other collection that allows duplicates.
     *
     * @param default Default value, will be used if not specified in the config file or config file is not present.
     * @param key JSON key of the [CollectionConfigOption] in the parent object.
     */
    @Suppress("unused")
    protected inline fun <reified T : Any, reified U : MutableCollection<T>> option(
        default: U, key: String
    ): CollectionConfigOption<T, U> {
        val kClass = T::class
        return CollectionConfigOption(kClass, default, key, top)
    }

    internal fun init() {
        val kClass = this::class

        for (innerclass in kClass.nestedClasses) {
            val obj = innerclass.objectInstance
            if (obj is ConfigFile) {
                throw Exception("You may not nest ConfigFile in ConfigFile")
            }
            if (obj is ConfigObject) {
                objectsMap[obj.key] = obj
                objectsList.add(obj)
                obj.init()
            }
        }

        HuebCraftConfigLib.LOGGER.debug("$kClass contains: {}", kClass.declaredMemberProperties)

        for (property in kClass.declaredMemberProperties) {
            property.isAccessible = true
            @Suppress("UNCHECKED_CAST") val delegate = (property as KProperty1<ConfigObject, *>).getDelegate(this)
            if (delegate != null) {
                if (delegate.javaClass.kotlin == ConfigOption::class || delegate.javaClass.kotlin.superclasses.contains(ConfigOption::class)) {
                    val opt = delegate as ConfigOption<*>
                    optionsMap[opt.key]
                    optionsList.add(opt)
                    if (delegate is CollectionConfigOption<*, *>) {
                        val any = delegate.get()
                        if (any is Collection<*>) {
                            any.forEach {
                                if (it != null) {
                                    if (it.javaClass.kotlin.superclasses.contains(ConfigObject::class)) {
                                        val confObj = it as ConfigObject
                                        confObj.init()
                                    }
                                }
                            }
                        }
                    } else if (delegate.getKClass().superclasses.contains(ConfigObject::class)) {
                        (delegate.get() as ConfigObject).init()
                    }
                }
            }
        }
    }

    internal fun getObjects(): List<ConfigObject> {
        return objectsList.toImmutable()
    }

    internal fun getOptions(): List<ConfigOption<*>> {
        return optionsList.toImmutable()
    }

    internal operator fun get(key: String): Any? {
        return get(key.splitToSequence("."))
    }

    private fun get(key: Sequence<String>): Any? {
        val first = key.first()
        return if (key.last() == first) {
            optionsMap[first]?.get()
        } else {
            objectsMap[first]?.get(key.drop(1))
        }
    }

    internal operator fun set(key: String, value: Any) {
        set(key.splitToSequence("."), value)
    }

    private fun set(key: Sequence<String>, value: Any) {
        val first = key.first()
        if (key.last() == first) {
            optionsMap[first]?.set(value)
        } else {
            objectsMap[first]?.set(key.drop(1), value)
        }
    }

    internal fun schedulePersist() = ConfigFileRegistry.scheduleSave(this)
}