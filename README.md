# HuebCraft ConfigLib

Based on [RedstoneParadox's ParadoxConfig](https://github.com/RedstoneParadox/ParadoxConfig)

## Features

- Provides a kotlin-ish interface for loading/storing configuration data in a json file
- Uses delegates
- Unlike ParadoxConfig, it supports collections of objects
- Uses GSON, no separate JSON library required, as it is bundled with Minecraft
- Is extendable with additional codecs via implementing the
  ``ConfigFormatInitializer`` interface in a Kotlin singleton (``object``) and configure the derived singleton
  in ``fabric.mod.json``
  to be loaded at the custom entrypoint ``hconfigFormat``.

## Usage

Add it to your mod's gradle buildscript:

```kotlin
repositories {
    // ...
    maven("https://repo.huebcraft.net/public-releases")
    // ...
}
// ...
dependencies {
    // ...
    modImplementation("de.huebcraft:configlib:1.0.4-SNAPSHOT")
    // ...
}
```

Implement the ``ConfigFile`` class:

```kotlin
import de.huebcraft.configlib.config.ConfigFile
import de.huebcraft.configlib.config.ConfigObject

object MyConfig : ConfigFile("myconfig.json") {
    var myString by option("default", key = "myString") // Simple string option, default value is "default"
    var myArray by option(
        mutableListOf("Apple", "Banana"),
        key = "fruits"
    ) // List option, default value is ["Apple", "Banana"]

    // Kotlin singleton object => JSON object with key "myObject" in the config file
    object MyObject : ConfigObject("myObject") {
        var stringInObject: String by option("default in object", key = "stringInObject") // String option in object
    }

    // Custom ConfigObject, may itself contain other ConfigOptions, including ConfigObjects
    class MyClass : ConfigObject() {
        var stringInClass: String by option("default in class", key = "stringInClass") // String option in class
    }

    val objectList by option(
        mutableListOf(MyClass(), MyClass()),
        key = "objectList"
    ) // List of objects, default value is a list of two instances of MyClass
}
```

This will create a config file with the following structure:

```json
{
    "myString": "default",
    "fruits": [
        "Apple",
        "Banana"
    ],
    "myObject": {
        "stringInObject": "default in object"
    },
    "objectList": [
        {
            "stringInClass": "default in class"
        },
        {
            "stringInClass": "default in class"
        }
    ]
}
```

Finally, add the following section to your mod's ``fabric.mod.json``:

```json5
{
    // ...
    "custom": {
         // ...
        "huebcraftconfiglib": {
            "package": "<package of your config file class>",
            "configs": [
                // Config class names
            ]
        }
    }
}
```
