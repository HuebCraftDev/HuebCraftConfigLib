package de.huebcraft.configlib

import de.huebcraft.configlib.codec.ConfigCodec
import de.huebcraft.configlib.config.ConfigFile
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.util.IllegalFormatException

object ConfigFileRegistry {
    private val configs: MutableMap<String, ConfigFile> = mutableMapOf()

    fun getConfig(id: String) : ConfigFile? {
        return configs[id]
    }

    internal fun initConfigs(rootPackage: String, configNames: Collection<String>, modid: String) {
        configNames.forEach {
            val className = "${rootPackage}.$it"

            when (val config = Class.forName(className).kotlin.objectInstance) {
                is ConfigFile -> {
                    config.init()
                    configs["$modid:${config.key}"]
                    loadConfig(config, modid)
                }
                null -> throw ClassNotFoundException("$className could not be found")
                else -> throw ClassCastException("$className does not extend ConfigFile")
            }
        }
    }

    @Throws(IllegalFormatException::class)
    private fun loadConfig(config: ConfigFile, modid: String) {
        val ext = config.fileName.split(".").last()
        val configCodec = ConfigCodec.getCodec(ext)
        val file = File(FabricLoader.getInstance().configDir.toFile(), "${modid}/${config.fileName}")

        if (file.exists()) {
            try {
                val configData = file.readText()

                configCodec.decode(configData, config)
                file.writeText(configCodec.encode(config))
            } catch (e: Exception) {
                Main.LOGGER.error("Could not write to config file $modid:${config.fileName} due to exception:", e)
            }
        } else {
            Main.LOGGER.info("Config file $modid:${config.fileName} was not found, creating a new one")

            try {
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeText(configCodec.encode(config))
            } catch (e: SecurityException) {
                Main.LOGGER.error("Could not write to config file $modid:${config.fileName} due to security violation:", e)
            } catch (e: Exception) {
                Main.LOGGER.error("Could not write to config file $modid:${config.fileName} due to exception:", e)
            }
        }
    }
}