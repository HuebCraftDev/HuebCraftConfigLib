package de.huebcraft.configlib

import de.huebcraft.configlib.Main.Companion.MOD_ID
import de.huebcraft.configlib.config.ConfigFile
import de.huebcraft.configlib.config.ConfigObject
import de.huebcraft.configlib.config.DevOnly

@DevOnly
@ConfigFile(MOD_ID, "test.json")
object TestConfig : ConfigObject() {
    var test by option("test", "test")

    var test2 by option(mutableSetOf("test"), "test2")
}