package de.huebcraft.configlib

import de.huebcraft.configlib.codec.ConfigCodec
import de.huebcraft.configlib.codec.GsonCodec
import de.huebcraft.configlib.config.*
import kotlinx.coroutines.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import net.minecraft.server.MinecraftServer
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.*

class Main : PreLaunchEntrypoint, ModInitializer, ServerLifecycleEvents.ServerStopping {
    companion object {
        const val MOD_ID = "huebcraftconfiglib"
        val LOGGER = LoggerFactory.getLogger(Main::class.java)!!
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
            LOGGER.info(it)
        }

        for (entrypoint in loader.getEntrypoints("hconfigFormat", ConfigFormatInitializer::class.java)) {
            entrypoint.initializeConfigFormat()
        }

        val reflections = Reflections(
            ConfigurationBuilder()
                .setUrls(
                    ClasspathHelper.forClassLoader(TestConfig::class.java.classLoader)
                    /**FabricLoader.getInstance().allMods
                    .filter { it.origin.kind != ModOrigin.Kind.NESTED }
                    .flatMap { it.origin.paths }
                    .map { it.toUri().toURL() }
                    .toTypedArray()*/
                )
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

        /*if (mod.metadata.containsCustomValue(MOD_ID)) {
            val obj = mod.metadata.getCustomValue(MOD_ID).asObject
            val rootPackage = obj.get("package").asString
            val classNames = obj.get("configs").asArray.map { it.asString }

            if (mod.metadata.id == MOD_ID && !FabricLoader.getInstance().isDevelopmentEnvironment) continue

            ConfigFileRegistry.initConfigs(rootPackage, classNames, mod.metadata.id)
        }*/
    }

    override fun onServerStopping(server: MinecraftServer?) {
        saveOnExit()
    }

    override fun onInitialize() {
        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                TestConfig.test = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                LOGGER.info(TestConfig.test2.spliterator().estimateSize().toString())
                TestConfig.test2 = mutableSetOf(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                delay(5000)
            }
        }
    }
}