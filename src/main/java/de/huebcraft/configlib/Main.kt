package de.huebcraft.configlib

import de.huebcraft.configlib.codec.ConfigCodec
import de.huebcraft.configlib.codec.GsonCodec
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import org.slf4j.LoggerFactory

object Main : PreLaunchEntrypoint {
    const val MOD_ID = "huebcraftconfiglib"
    val LOGGER = LoggerFactory.getLogger(Main::class.java)

    @Suppress("unused")
    override fun onPreLaunch() {
        val loader = FabricLoader.getInstance()

        ConfigCodec.addCodec(GsonCodec())

        LOGGER.info("Available extensions:")
        ConfigCodec.codecs.keys.forEach {
            LOGGER.info(it)
        }

        for (entrypoint in loader.getEntrypoints("hconfigFormat", ConfigFormatInitializer::class.java)) {
            entrypoint.initializeConfigFormat()
        }

        for (mod in loader.allMods) {
            if (mod.metadata.containsCustomValue(MOD_ID)) {
                val obj = mod.metadata.getCustomValue(MOD_ID).asObject
                val rootPackage = obj.get("package").asString
                val classNames = obj.get("configs").asArray.map { it.asString }

                if (mod.metadata.id == MOD_ID && !FabricLoader.getInstance().isDevelopmentEnvironment) continue

                ConfigFileRegistry.initConfigs(rootPackage, classNames, mod.metadata.id)
            }
        }
    }
}