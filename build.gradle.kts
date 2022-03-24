plugins {
    id("fabric-loom") version ("0.12.5")
    kotlin("jvm") version ("1.6.10")
    id("maven-publish")
}

val modVersion: String by project
val minecraftVersion: String by project
val loaderVersion: String by project
val fabricVersion: String by project
val fabricKotlinVersion: String by project
val yarnMappings: String by project
val mavenGroup: String by project
val archivesBaseName: String by project

project.version = modVersion
project.group = mavenGroup
project.base.archivesName.set(archivesBaseName)

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings("net.fabricmc:yarn:${yarnMappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${fabricKotlinVersion}")
}

val targetJavaVersion = 17
tasks {
    withType(ProcessResources::class) {
        inputs.property("version", project.version)
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }

    withType(Jar::class) {
        from("LICENSE") {
            rename { "${it}_${project.base.archivesName}" }
        }
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }

    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = project.base.archivesName.get()
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "HuebCraft"
            url = uri("http://85.214.197.24:9000/")
            isAllowInsecureProtocol = true
            credentials {
                val mavenUser: String by project
                val mavenPassword: String by project
                username = mavenUser
                password = mavenPassword
            }
            authentication {
                create<BasicAuthentication>("huebcraftPublish")
            }
        }
    }
}