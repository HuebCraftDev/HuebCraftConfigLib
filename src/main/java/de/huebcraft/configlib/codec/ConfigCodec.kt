package de.huebcraft.configlib.codec

import de.huebcraft.configlib.config.ConfigFile

interface ConfigCodec {
    val fileExtension: String

    fun decode(data: String, configFile: ConfigFile): ConfigFile
    fun encode(configFile: ConfigFile): String

    companion object {
        internal val codecs: MutableMap<String, ConfigCodec> = mutableMapOf()

        @Throws(Exception::class)
        fun addCodec(configCodec: ConfigCodec) {
            val ext = configCodec.fileExtension

            if (codecs.containsKey(ext)) {
                throw Exception("ConfigCodec for file extension $ext was already registered!")
            }

            codecs[ext] = configCodec
        }

        fun getCodec(ext: String): ConfigCodec {
            return codecs.getOrElse(ext) {
                throw Exception("No ConfigCodec for file extension $ext found")
            }
        }
    }
}