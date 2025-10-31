import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.GradleException
import java.util.Locale
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
    kotlin("jvm") version "2.2.20"
    id("fabric-loom") version "1.11.8"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 17
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.enginehub.org/repo")
}

val minecraftVersion = project.property("minecraft_version") as String? ?: "1.20.4"
val worldeditVersion = project.property("worldedit_version") as String? ?: "7.3.0"

dependencies {
    // Use the classic property name value — this ensures Loom sees the intended Minecraft artifact.
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")
    modImplementation("com.sk89q.worldedit:worldedit-core:${worldeditVersion}")

    // Adventure NBT
    implementation("net.kyori:adventure-nbt:4.25.0")
    implementation("net.kyori:adventure-api:4.25.0")
    implementation("net.kyori:examination-api:1.3.0")
    implementation("net.kyori:examination-string:1.3.0")
    include("net.kyori:adventure-nbt:4.25.0")
    include("net.kyori:adventure-api:4.25.0")
    include("net.kyori:examination-api:1.3.0")
    include("net.kyori:examination-string:1.3.0")
}

tasks.processResources {
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

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
            pom {
                name.set(project.property("archives_base_name") as String)
                description.set("Fabric mod integrating WorldEdit Core for Minecraft $minecraftVersion")
                url.set("https://github.com/erikd/lite2edit")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("erikd")
                        name.set("Erik D")
                        email.set("erikd@example.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/erikd/lite2edit.git")
                    developerConnection.set("scm:git:ssh://github.com:erikd/lite2edit.git")
                    url.set("https://github.com/erikd/lite2edit")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/erikd/lite2edit")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: (findProperty("gpr.user") as String?)
                password = System.getenv("GITHUB_TOKEN") ?: (findProperty("gpr.key") as String?)
            }
        }
    }
}