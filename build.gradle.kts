import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.21"
    id("fabric-loom") version "1.11.8"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.enginehub.org/repo")
}

val minecraftVersion = (project.findProperty("minecraft_version") as String?) ?: "1.20.4"
val worldeditVersion = (project.findProperty("worldedit_version") as String?) ?: "7.3.0"

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")
    modImplementation("com.sk89q.worldedit:worldedit-fabric-mc$minecraftVersion:$worldeditVersion")

    implementation("net.kyori:adventure-nbt:4.25.0")
    implementation("net.kyori:adventure-api:4.25.0")
    implementation("net.kyori:examination-api:1.3.0")
    implementation("net.kyori:examination-string:1.3.0")

    include("net.kyori:adventure-nbt:4.25.0")
    include("net.kyori:adventure-api:4.25.0")
    include("net.kyori:examination-api:1.3.0")
    include("net.kyori:examination-string:1.3.0")
}

tasks {
    processResources {
        inputs.property("version", project.version)
        inputs.property("minecraft_version", minecraftVersion)
        inputs.property("loader_version", project.property("loader_version"))
        inputs.property("kotlin_loader_version", project.property("kotlin_loader_version"))
        inputs.property("worldedit_version", worldeditVersion)

        filteringCharset = "UTF-8"
        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "minecraft_version" to minecraftVersion,
                "loader_version" to project.property("loader_version"),
                "kotlin_loader_version" to project.property("kotlin_loader_version"),
                "worldedit_version" to worldeditVersion
            )
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${project.base.archivesName}" }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Lite2Edit")
                description.set("Fabric mod for editing Litematica schematics")
                url.set("https://github.com/Erik-Donath/Lite2Edit")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("erik-donath")
                        name.set("Erik Donath")
                    }
                }

                scm {
                    url.set("https://github.com/Erik-Donath/Lite2Edit")
                    connection.set("scm:git:git://github.com/Erik-Donath/Lite2Edit.git")
                    developerConnection.set("scm:git:ssh://github.com/Erik-Donath/Lite2Edit.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Erik-Donath/Lite2Edit")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
