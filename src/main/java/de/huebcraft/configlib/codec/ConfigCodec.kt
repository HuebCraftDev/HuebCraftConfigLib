package de.huebcraft.configlib.codec

import de.huebcraft.configlib.config.ConfigFile
import de.huebcraft.configlib.config.ConfigObject
import de.huebcraft.configlib.config.ConfigOption

/**
 * A codec is responsible for encoding and decoding a [ConfigFile] to and from a specific format.
 * It must be able to handle all types of [ConfigOption]s, including collections of [ConfigObject]s.
 */
interface ConfigCodec {
    /**
     * File extension of the format this codec can encode/decode to/from.
     * Must be overridden by subclasses.
     */
    val fileExtension: String

    /**
     * Decodes [data] into the given instance of [ConfigFile].
     *
     * @param data A valid JSON string to decode.
     * @param configFile The [ConfigFile] to decode into, old values will be overwritten.
     *
     * @return A reference to the passed [ConfigFile] instance to integrate the method better.
     */
    fun decode(data: String, configFile: ConfigFile): ConfigFile

    /**
     * Encodes the given [ConfigFile] into a valid, pretty-printed JSON string.
     *
     * @param configFile [ConfigFile] to encode.
     * @return A valid, pretty-printed JSON string.
     */
    fun encode(configFile: ConfigFile): String

    companion object {
        internal val codecs: MutableMap<String, ConfigCodec> = mutableMapOf()

        /**
         * Registers a new [ConfigCodec] to be used by the library.
         *
         * @param configCodec [ConfigCodec] to register.
         * @throws IllegalArgumentException If the [ConfigCodec] is already registered.
         */
        @Throws(Exception::class)
        fun addCodec(configCodec: ConfigCodec) {
            val ext = configCodec.fileExtension

            if (codecs.containsKey(ext)) {
                throw Exception("ConfigCodec for file extension $ext was already registered!")
            }

            codecs[ext] = configCodec
        }

        /**
         * Returns the [ConfigCodec] for the given file extension.
         *
         * @param ext File extension to get the [ConfigCodec] for.
         * @return [ConfigCodec] for the given file extension.
         * @throws IllegalArgumentException If no [ConfigCodec] is registered for the given file extension.
         */
        @Throws(Exception::class)
        fun getCodec(ext: String): ConfigCodec {
            return codecs.getOrElse(ext) {
                throw Exception("No ConfigCodec for file extension $ext found")
            }
        }
    }
}