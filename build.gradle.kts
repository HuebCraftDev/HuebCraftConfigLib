plugins {
    id("fabric-loom") version ("0.13-SNAPSHOT")
    kotlin("jvm") version ("1.7.10")
    id("maven-publish")
}

val modVersion =
    System.getenv("CI_COMMIT_TAG") ?: System.getenv("CI_COMMIT_SHORT_SHA")?.let { "$it-dev" } ?: "0.0.0-SNAPSHOT"
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

    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.mockito:mockito-inline:4.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

val targetJavaVersion = 17
tasks {
    @Suppress("UnstableApiUsage")
    withType<ProcessResources> {
        inputs.property("version", project.version)
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "fabricKotlinVersion" to fabricKotlinVersion,
            )
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }

    withType<Jar> {
        from("LICENSE") {
            rename { "${it}_$archivesBaseName" }
        }
    }

    withType<Test> {
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
        create<MavenPublication>("mavenJava") {
            version = project.version as String

            from(components["java"])
        }
    }

    repositories {
        if (System.getenv("CI_JOB_TOKEN") != null) {
            maven {
                name = "GitLab"
                val projectId = System.getenv("CI_PROJECT_ID")
                val apiV4 = System.getenv("CI_API_V4_URL")
                url = uri("$apiV4/projects/$projectId/packages/maven")
                authentication {
                    create("token", HttpHeaderAuthentication::class.java) {
                        credentials(HttpHeaderCredentials::class.java) {
                            name = "Job-Token"
                            value = System.getenv("CI_JOB_TOKEN")
                        }
                    }
                }
            }
        }
    }
}

