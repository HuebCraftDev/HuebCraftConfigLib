package de.huebcraft.configlib.config

object TestConfig : ConfigFile("test.json") {
    var test: String by option("test", "test")
}