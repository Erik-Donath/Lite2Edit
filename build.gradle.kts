import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.21"
    id("fabric-loom") version "1.11.8"
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.+"
}

// Dynamic values from project properties (set by workflow or defaults)
val modVersion = project.findProperty("mod_version") as String? ?: "0.4"
val minecraftVersion = project.findProperty("minecraft_version") as String? ?: "1.20.4"
val archiveBaseName = project.findProperty("archives_base_name") as String? ?: "lite2edit"

// Read versions from gradle.properties
val loaderVersion = project.findProperty("loader_version") as String? ?: "0.17.3"
val kotlinLoaderVersion = project.findProperty("kotlin_loader_version") as String? ?: "1.13.7+kotlin.2.2.21"
val worldEditVersion = project.findProperty("worldedit_version") as String? ?: "7.3.0"

version = modVersion
group = "io.github.erik-donath"

base {
    archivesName.set(archiveBaseName)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.enginehub.org/repo")
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")

    // Kotlin support
    modImplementation("net.fabricmc:fabric-language-kotlin:${kotlinLoaderVersion}")

    // WorldEdit core API
    modCompileOnly("com.sk89q.worldedit:worldedit-core:${worldEditVersion}")

    // Runtime dependencies
    implementation("net.kyori:adventure-nbt:4.25.0")
    implementation("net.kyori:adventure-api:4.25.0")
    implementation("net.kyori:examination-api:1.3.0")
    implementation("net.kyori:examination-string:1.3.0")

    include("net.kyori:adventure-nbt:4.25.0")
    include("net.kyori:adventure-api:4.25.0")
    include("net.kyori:examination-api:1.3.0")
    include("net.kyori:examination-string:1.3.0")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("loader_version", loaderVersion)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("kotlin_loader_version", kotlinLoaderVersion)
    inputs.property("worldedit_version", worldEditVersion)

    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "loader_version" to loaderVersion,
            "minecraft_version" to minecraftVersion,
            "kotlin_loader_version" to kotlinLoaderVersion,
            "worldedit_version" to worldEditVersion
        )
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(System.getenv("MODRINTH_ID") ?: "")

    // Version configuration
    versionNumber.set("${modVersion}-mc${minecraftVersion}")
    versionName.set("Lite2Edit ${modVersion} for Minecraft ${minecraftVersion}")
    versionType.set("release")

    // File to upload
    uploadFile.set(tasks.remapJar)

    // Game versions and loaders
    gameVersions.addAll(minecraftVersion)
    loaders.add("fabric")

    // Changelog
    val includeChangelog = System.getenv("INCLUDE_CHANGELOG")?.toBoolean() ?: false
    if (includeChangelog) {
        changelog.set(
            loadChangelog(modVersion)
        )
    }

    // Dependencies
    dependencies {
        required.project("fabric-language-kotlin")
        required.project("worldedit")
    }

    failSilently.set(true)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "io.github.erik-donath"
            artifactId = "lite2edit-mc${minecraftVersion}"
            version = modVersion

            pom {
                name.set("Lite2Edit")
                description.set("A Fabric mod that lets WorldEdit open Litematica schematic files (.litematic / .ltc). Converts Litematica schematics into WorldEdit clipboards for easy importing and editing.")
                url.set("https://github.com/Erik-Donath/lite2edit")
                inceptionYear.set("2025")

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                        comments.set("A permissive license that is short and to the point.")
                    }
                }

                developers {
                    developer {
                        id.set("erik-donath")
                        name.set("Erik Donath")
                        email.set("erik.donath@gmail.com")
                        url.set("https://github.com/Erik-Donath")
                        roles.set(listOf("developer", "maintainer"))
                        timezone.set("Europe/Berlin")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Erik-Donath/lite2edit.git")
                    developerConnection.set("scm:git:ssh://github.com/Erik-Donath/lite2edit.git")
                    url.set("https://github.com/Erik-Donath/lite2edit/tree/master")
                    tag.set("HEAD")
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/Erik-Donath/lite2edit/issues")
                }

                ciManagement {
                    system.set("GitHub Actions")
                    url.set("https://github.com/Erik-Donath/lite2edit/actions")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Erik-Donath/lite2edit")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.token") as String?
            }
        }
    }
}

// Task to print changelog for GitHub Actions
tasks.register("printChangelog") {
    doLast {
        val version = project.findProperty("mod_version") as String? ?: "0.4"
        println(loadChangelog(version))
    }
}

// Helper function to load changelog
fun loadChangelog(version: String): String {
    val changelogFile = file("CHANGELOG.md")
    if (!changelogFile.exists()) return "No changelog available for version $version"

    try {
        val content = changelogFile.readText()
        val lines = content.lines()
        var found = false
        val changelogLines = mutableListOf<String>()

        for (line in lines) {
            if (line.startsWith("## Version") && line.contains(version)) {
                found = true
                changelogLines.add(line)
                continue
            }

            if (found) {
                if ((line.startsWith("## Version") && !line.contains(version)) ||
                    line.startsWith("---")) {
                    break
                }

                if (changelogLines.size > 1 || line.trim().isNotEmpty()) {
                    changelogLines.add(line)
                }
            }
        }

        val result = changelogLines.joinToString("\n").trim()
        return if (result.isNotEmpty()) {
            result.take(65000) // Modrinth changelog limit
        } else {
            "## Version $version\n- Multi-version release for Minecraft $version\n- Cross-platform WorldEdit compatibility\n- Enhanced Litematica schematic conversion"
        }
    } catch (e: Exception) {
        return "Error loading changelog for version $version: ${e.message}"
    }
}
