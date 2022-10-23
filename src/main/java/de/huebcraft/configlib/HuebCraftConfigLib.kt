package de.huebcraft.configlib

import de.huebcraft.configlib.codec.ConfigCodec
import de.huebcraft.configlib.codec.GsonCodec
import de.huebcraft.configlib.config.*
import kotlinx.coroutines.*
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import net.minecraft.server.MinecraftServer
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.io.path.*

class HuebCraftConfigLib : PreLaunchEntrypoint, ServerLifecycleEvents.ServerStopping {
    companion object {
        val LOGGER = LoggerFactory.getLogger(HuebCraftConfigLib::class.java)!!
    }

    private fun saveOnExit() {
        LOGGER.info("Saving configs on exit")
        ConfigFileRegistry.saveConfigs()
    }

    @Suppress("unused")
    override fun onPreLaunch() {
        val loader = FabricLoader.getInstance()

        ServerLifecycleEvents.SERVER_STOPPING.register(this)

        ConfigCodec.addCodec(GsonCodec())

        LOGGER.info("Available extensions:")
        ConfigCodec.codecs.keys.forEach {
            LOGGER.info(" - $it")
        }

        for (entrypoint in loader.getEntrypoints("hconfigFormat", ConfigFormatInitializer::class.java)) {
            entrypoint.initializeConfigFormat()
        }

        val reflections = Reflections(
            ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClassLoader(HuebCraftConfigLib::class.java.classLoader))
                .addScanners(Scanners.TypesAnnotated)
        )

        val configClasses = reflections.getTypesAnnotatedWith(ConfigFile::class.java)
            .map {
                val className = it.name
                @Suppress("UNCHECKED_CAST")
                it as? Class<out ConfigObject>
                    ?: throw IllegalStateException("Config class $className is not a subclass of ConfigObject")
            }
            .associateWith { it.kotlin.objectInstance }
            .filter { it.value != null }
            .mapValues { it.value!! }
            .filter { config ->
                when (FabricLoader.getInstance().environmentType) {
                    EnvType.CLIENT -> config.key.annotations.any { it is ClientOnly }
                            || !config.key.annotations.any { it is ServerOnly }

                    EnvType.SERVER -> config.key.annotations.any { it is ServerOnly }
                            || !config.key.annotations.any { it is ClientOnly }

                    else -> {
                        LOGGER.error("Environment type is null")
                        false
                    }
                }
            }
            .filter { config ->
                if (!FabricLoader.getInstance().isDevelopmentEnvironment) {
                    !config.key.annotations.any { it is DevOnly }
                } else {
                    true
                }
            }

        if (configClasses.isNotEmpty()) {
            LOGGER.info("Found config objects:")
            configClasses.forEach {
                LOGGER.info(" - ${it.key.name}")
            }

            ConfigFileRegistry.initConfigs(configClasses.keys)
        }
    }

    override fun onServerStopping(server: MinecraftServer?) = saveOnExit()
}