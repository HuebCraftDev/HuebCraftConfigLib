package de.huebcraft.configlib

import de.huebcraft.configlib.codec.ConfigCodec

/**
 * Other mods can subclass this to register their own [ConfigCodec]s.
 * Add the subclasses of your mod to the "hconfigFormat" entrypoint
 */
interface ConfigFormatInitializer {
    /**
     * Register your [ConfigCodec]s here.
     * This method is called by ConfigLib before regular initialization, but after the preLaunch entrypoint.
     */
    fun initializeConfigFormat()
}