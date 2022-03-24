package de.huebcraft.configlib.codec

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import de.huebcraft.configlib.Main
import de.huebcraft.configlib.config.CollectionConfigOption
import de.huebcraft.configlib.config.ConfigFile
import de.huebcraft.configlib.config.ConfigObject
import de.huebcraft.configlib.config.ConfigOption
import net.minecraft.util.Identifier
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.superclasses

class GsonCodec : ConfigCodec {
    override val fileExtension = "json"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Identifier::class.java, object : TypeAdapter<Identifier>() {
            val EMPTY: Identifier = Identifier("empty")

            override fun write(out: JsonWriter?, value: Identifier?) {
                out?.value(value.toString())
            }

            override fun read(`in`: JsonReader?): Identifier {
                return Identifier.tryParse(`in`?.nextString()) ?: EMPTY
            }
        })
        .setPrettyPrinting()
        .create()

    @Throws(Exception::class)
    override fun decode(data: String, configFile: ConfigFile): ConfigFile {
        val json = gson.fromJson(data, JsonObject::class.java)

        return decodeObject(json, configFile) as ConfigFile // Will return configFile
    }

    override fun encode(configFile: ConfigFile): String {
        return gson.toJson(encodeObject(configFile))
    }

    private fun encodeObject(configObj: ConfigObject): JsonObject {
        val obj = JsonObject()

        Main.LOGGER.info(configObj.optionsMap.values.toString())

        for (option in configObj.getOptions().sortedBy { it.getKClass().superclasses.contains(ConfigObject::class) }) {
            Main.LOGGER.info("Encoding option ${option.key} of object $configObj")
            if (option.wasLoadedFromFile) {
                val optionElement = encodeOption(option)

                obj.add(option.key, optionElement)
            }
        }

        for (subobj in configObj.getObjects()) {
            Main.LOGGER.info("Encoding subobject ${subobj.key} of object $configObj")
            val subobjObj = encodeObject(subobj)

            obj.add(subobj.key, subobjObj)
        }

        return obj;
    }

    private fun encodeOption(option: ConfigOption<*>): JsonElement {
        val any = option.get()

        if (option is CollectionConfigOption<*, *> && any is Collection<*>) {
            Main.LOGGER.info("Option is Collection<*>")
            val array = JsonArray()

            if (option.getElementKClass().superclasses.contains(ConfigObject::class)) {
                Main.LOGGER.info("Option is Collection<ConfigObject>")
                any.forEach {
                    array.add(encodeObject(it as ConfigObject))
                }
            } else {
                Main.LOGGER.info("Option is Collection<Any>")
                any.forEach {
                    array.add(gson.toJsonTree(it, option.getElementKClass().java))
                }
            }
            return array
        }

        if (option.getKClass().superclasses.contains(ConfigObject::class) && any is ConfigObject) {
            Main.LOGGER.info("Option is ConfigObject")
            return encodeObject(any)
        }

        Main.LOGGER.info(
            "Option ${option.key} ($any) encoded to: ${
                gson.toJson(
                    gson.toJsonTree(
                        any,
                        option.getKClass().java
                    )
                )
            }"
        )

        return gson.toJsonTree(any, option.getKClass().java)
    }

    @Throws(Exception::class)
    private fun decodeObject(json: JsonObject, obj: ConfigObject): ConfigObject {
        for (subobj in obj.getObjects()) {
            val jsonObj = json[subobj.key]

            if (jsonObj is JsonObject) {
                decodeObject(jsonObj, subobj)
            }
        }

        for (option in obj.getOptions()) {
            val element = json[option.key]

            if (element != null) {
                decodeOption(element, option)
            } else {
                option.wasLoadedFromFile = false
            }
        }

        return obj
    }

    @Throws(Exception::class)
    private fun decodeOption(json: JsonElement, option: ConfigOption<*>): ConfigOption<*> {
        if (json is JsonArray && option is CollectionConfigOption<*, *>) {
            val collection = mutableListOf<Any>()

            if (option.getElementKClass().superclasses.contains(ConfigObject::class)) {
                json.forEach {
                    if (it is JsonObject) {
                        val confObj = option.getElementKClass().createInstance() as ConfigObject
                        confObj.init()
                        collection.add(decodeObject(it, confObj))
                    } else {
                        throw Exception("Expected json object array for ${option.key}")
                    }
                }
            } else {
                json.forEach {
                    collection.add(gson.fromJson(it, option.getElementKClass().java))
                }
            }
            option.set(collection)
        } else if (json is JsonObject && option.getKClass().superclasses.contains(ConfigObject::class)) {
            val confObj = option.getKClass().createInstance() as ConfigObject
            confObj.init()
            option.set(decodeObject(json, confObj))
        } else {
            val any = gson.fromJson(json, option.getKClass().java)

            option.set(any)
        }

        return option
    }
}