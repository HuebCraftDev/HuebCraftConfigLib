plugins {
    id("fabric-loom") version ("0.12.5")
    kotlin("jvm") version ("1.7.0")
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

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.mockito:mockito-core:4.4.0")
    testImplementation("org.mockito:mockito-inline:4.4.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
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

    withType(Test::class) {
        useJUnitPlatform()
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }

    withSourcesJar()
    withJavadocJar()
}
repositories {
    mavenCentral()
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
            url = uri("https://repo.huebcraft.net")
            isAllowInsecureProtocol = true
            credentials {
                var mavenUser = ""
                var mavenPassword = ""
                if (project.hasProperty("mavenUser")) {
                    println("Using property for maven user")
                    mavenUser = project.property("mavenUser").toString()
                }
                if (project.hasProperty("mavenPassword")) {
                    println("Using property for maven password")
                    mavenPassword = project.property("mavenPassword").toString()
                }
                if (mavenUser.isBlank()) {
                    println("Using environment variables for maven user")
                    mavenUser = System.getenv("MAVEN_USER")
                }
                if (mavenPassword.isBlank()) {
                    println("Using environment variables for maven password")
                    mavenPassword = System.getenv("MAVEN_PASSWORD")
                }
                username = mavenUser
                password = mavenPassword
            }
            authentication {
                create<BasicAuthentication>("huebcraftPublish")
            }
        }
    }
}
