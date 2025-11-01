import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.21"
    id("fabric-loom") version "1.11.8"
    id("maven-publish")
}


val modVersion = project.findProperty("mod_version") as String? ?: "0.3"
val minecraftVersion = project.findProperty("minecraft_version") as String? ?: "1.20.4"
val archiveBaseName = project.findProperty("archives_base_name") as String? ?: "lite2edit"

val loaderVersion = project.findProperty("loader_version") as String? ?: "0.17.3"
val kotlinLoaderVersion = project.findProperty("kotlin_loader_version") as String? ?: "1.13.7+kotlin.2.2.21"
val worldEditVersion = project.findProperty("worldedit_version") as String? ?: "7.3.0"

version = modVersion
group = "io.github.erik-donath"

base {
    archivesName.set(archiveBaseName)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
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
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("loader_version", loaderVersion)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("kotlin_loader_version", kotlinLoaderVersion)
    inputs.property("worldedit_version", worldEditVersion)

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

                contributors {}

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

                properties.set(mapOf(
                    "maven.compiler.source" to "21",
                    "maven.compiler.target" to "21",
                    "project.build.sourceEncoding" to "UTF-8",
                    "minecraft.version" to minecraftVersion,
                    "mod.loader" to "fabric",
                    "fabric.loader.version" to loaderVersion,
                    "fabric.kotlin.version" to kotlinLoaderVersion,
                    "worldedit.version" to worldEditVersion
                ))
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
