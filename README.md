# HuebCraft ConfigLib
Based on [RedstoneParadox's ParadoxConfig](https://github.com/RedstoneParadox/ParadoxConfig)

## Features
- Provides a kotlin-ish interface for loading/storing configuration data in a json file
- Uses delegates
- Unlike ParadoxConfig, it supports collections of objects
- Uses GSON, no separate JSON library required, as it is bundled with Minecraft
- Is extendable with additional codecs via implementing the ``ConfigFormatInitializer`` interface
  in a Kotlin singleton (``object``) and configure the derived singleton in ``fabric.mod.json``
  to be loaded at the ``hconfigFormat`` custom stage
