package de.huebcraft.configlib

import de.huebcraft.configlib.codec.ConfigCodec
import de.huebcraft.configlib.codec.GsonCodec
import de.huebcraft.configlib.config.TestConfig
import net.fabricmc.loader.api.FabricLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.File
import java.nio.file.Path

class SaveOnExitTest {
    @Test
    fun testSaveOnExit() {
        ConfigCodec.addCodec(GsonCodec())
        Mockito.mockStatic(FabricLoader::class.java).use {
            val loaderMock = Mockito.mock(FabricLoader::class.java)

            Mockito.`when`(loaderMock.configDir).thenReturn(Path.of("."))
            it.`when`<FabricLoader>(FabricLoader::getInstance).thenReturn(loaderMock)

            ConfigFileRegistry.initConfigs(listOf(TestConfig::class.java))

            assert(File("./test/test.json").exists())
            Assertions.assertEquals(TestConfig.test, "test")

            TestConfig.test = "test2"
            ConfigFileRegistry.saveConfigs()

            Assertions.assertEquals("test2", TestConfig.test)
            Assertions.assertEquals("{\n  \"test\": \"test2\"\n}", File("./test/test.json").readText())
        }
    }

    @AfterEach
    fun cleanUp() {
        File("./test").deleteRecursively()
    }
}