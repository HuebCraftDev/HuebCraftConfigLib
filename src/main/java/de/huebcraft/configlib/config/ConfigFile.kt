package de.huebcraft.configlib.config

/**
 * A subclass of [ConfigObject] that represents a config file.
 *
 * Config files will be stored in &lt;server root&gt;/config/&lt;modid&gt;/&lt;file name&gt;.
 * Changes are not saved at the moment, but this feature will come in future versions.
 *
 * @param fileName Filename of the config file.
 */
abstract class ConfigFile(val fileName: String) : ConfigObject()