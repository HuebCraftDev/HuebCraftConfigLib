package de.huebcraft.configlib.config

@ConfigFile("test", "test.json")
object TestConfig : ConfigObject() {
    var test: String by option("test", "test")
}