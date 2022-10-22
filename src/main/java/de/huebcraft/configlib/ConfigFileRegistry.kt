package de.huebcraft.configlib

import de.huebcraft.configlib.codec.ConfigCodec
import de.huebcraft.configlib.config.ConfigObject
import kotlinx.coroutines.*
import net.fabricmc.loader.api.FabricLoader
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.*

object ConfigFileRegistry {
    private val configs: MutableMap<String, MutableMap<String, ConfigFileEntry>> = mutableMapOf()
    private val LOGGER = LogManager.getLogger(ConfigFileRegistry::class.java)

    private data class ConfigFileEntry(
        val configFile: ConfigObject,
        val fileName: String,
        var saveJob: Job? = null
    )

    /**
     * May be used to manually retrieve a config file.
     * Should not be used in general, as config files are automatically loaded
     * into the singletons of their [ConfigObject] implementations.
     *
     * @param id Id of the config file (&lt;modid&gt;:&lt;name including extension&gt;)
     */
    @Suppress("unused")
    fun getConfig(id: String): ConfigObject? {
        return configs[id.split(":")[0]]?.get(id.split(":")[1])?.configFile
    }

    internal fun exists(file: ConfigObject) =
        configs.any { entry -> entry.value.any { it.value.configFile == file } }

    internal fun initConfigs(configClasses: Collection<Class<out ConfigObject>>) {
        configClasses.forEach {
            LOGGER.info("Loading config ${it.canonicalName}")

            when (val config = it.kotlin.objectInstance) {
                is ConfigObject -> {
                    config.init()
                    if (configs[config.modId] == null) {
                        configs[config.modId] = mutableMapOf()
                    }
                    configs[config.modId]!![config.key] =
                        ConfigFileEntry(loadConfig(config), config.fileName)
                }

                else -> throw ClassCastException("${it.canonicalName} is not a object extending ConfigFile")
            }
        }
    }

    @Throws(IllegalFormatException::class)
    private fun loadConfig(config: ConfigObject): ConfigObject {
        val ext = config.fileName.split(".").last()
        val configCodec = ConfigCodec.getCodec(ext)
        val file = File(FabricLoader.getInstance().configDir.toFile(), "${config.modId}/${config.fileName}")

        if (file.exists()) {
            try {
                val configData = file.readText()

                configCodec.decode(configData, config)
                file.writeText(configCodec.encode(config))
            } catch (e: Exception) {
                LOGGER.error("Could not write to config file ${config.modId}:${config.fileName} due to exception:", e)
            }
        } else {
            LOGGER.info("Config file ${config.modId}:${config.fileName} was not found, creating a new one")

            try {
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeText(configCodec.encode(config))
            } catch (e: SecurityException) {
                LOGGER.error(
                    "Could not write to config file ${config.modId}:${config.fileName} due to security violation:", e
                )
            } catch (e: Exception) {
                LOGGER.error("Could not write to config file ${config.modId}:${config.fileName} due to exception:", e)
            }
        }
        return config
    }

    internal fun scheduleSave(file: ConfigObject) {
        val modConfigs =
            configs.entries.firstOrNull { entry -> entry.value.values.any { it.configFile == file } } ?: return
        LOGGER.info("Scheduling save of config file ${file.key}")
        val configEntry = modConfigs.value.entries.first { it.value.configFile == file }.value
        val modId = modConfigs.key

        synchronized(configEntry.configFile) {
            configEntry.saveJob?.cancel()
            configEntry.saveJob = CoroutineScope(Dispatchers.IO).launch {
                delay(800)
                saveConfig(file, modId)
            }
        }
    }

    internal fun saveConfigs() {
        configs.forEach { (modId, configMap) ->
            configMap.forEach { (_, config) ->
                synchronized(config.configFile) {
                    saveConfig(config.configFile, modId)
                }
            }
        }
    }

    private fun saveConfig(config: ConfigObject, modId: String): ConfigObject {
        val ext = config.fileName.split(".").last()
        val configCodec = ConfigCodec.getCodec(ext)
        val file = File(FabricLoader.getInstance().configDir.toFile(), "${modId}/${config.fileName}")

        if (file.exists()) {
            try {
                file.writeText(configCodec.encode(config))
            } catch (e: Exception) {
                LOGGER.error("Could not write to config file $modId:${config.fileName} due to exception:", e)
            }
        } else {
            LOGGER.info("Config file $modId:${config.fileName} was not found, creating a new one")

            try {
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeText(configCodec.encode(config))
            } catch (e: SecurityException) {
                LOGGER.error(
                    "Could not write to config file $modId:${config.fileName} due to security violation:", e
                )
            } catch (e: Exception) {
                LOGGER.error("Could not write to config file $modId:${config.fileName} due to exception:", e)
            }
        }
        return config
    }
}