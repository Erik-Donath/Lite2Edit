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

// Use mod_version from gradle.properties as the published version
version = (findProperty("mod_version") as String?) ?: "0.1.0"
group = (findProperty("maven_group") as String?) ?: "de.erikd"
base.archivesName.set((findProperty("archives_base_name") as String?) ?: project.name)

// default fallback for MC if nothing provided
val defaultMc = (findProperty("default_minecraft_version") as String?) ?: "1.20.4"

// IMPORTANT: Fabric/Loom expects the legacy project property minecraft_version to exist.
// Provide the value to the build logic by preferring the classic property name first.
// This will be satisfied by gradle.properties or by passing -Pminecraft_version=<version> on the CLI.
//
// For convenience we also accept -PminecraftVersion (camelCase) and MINECRAFT_VERSION env var.
val minecraftVersion = when {
    project.hasProperty("minecraft_version") -> project.property("minecraft_version").toString()
    project.hasProperty("minecraftVersion") -> project.property("minecraftVersion").toString()
    System.getenv("MINECRAFT_VERSION") != null -> System.getenv("MINECRAFT_VERSION")
    else -> defaultMc
}.trim()

// WorldEdit selection: default to latest non-SNAPSHOT release. Override with -PworldeditVersion or WORLDEDIT_VERSION
val defaultWorldEdit = (findProperty("worldedit_version") as String?) ?: "latest.release"
val worldeditVersion = when {
    project.hasProperty("worldeditVersion") -> project.property("worldeditVersion").toString()
    System.getenv("WORLDEDIT_VERSION") != null -> System.getenv("WORLDEDIT_VERSION")
    else -> defaultWorldEdit
}.trim()

// Toolchain target
val targetJavaVersion = 17
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

// artifactId: <archives_base_name>-fabric-<mcVersion> (lowercased to avoid GitHub Packages normalization issues)
val archivesBase = (findProperty("archives_base_name") as String?) ?: project.name
val artifactIdFinal = "${archivesBase}-fabric-${minecraftVersion}".lowercase(Locale.ROOT)

repositories {
    mavenCentral()
    maven { url = uri("https://maven.enginehub.org/repo/") }

    // Snapshot repos (allow resolution of transitive SNAPSHOT dependencies if necessary)
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://maven.enginehub.org/snapshots/") }
}

dependencies {
    // Use the classic property name value — this ensures Loom sees the intended Minecraft artifact.
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    // WorldEdit artifact that matches the MC version
    modImplementation("com.sk89q.worldedit:worldedit-fabric-mc${minecraftVersion}:$worldeditVersion")
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
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

// Print which WorldEdit artifact gets selected for the chosen Minecraft version
tasks.register("printWorldEditVersion") {
    group = "help"
    description = "Resolve and print the selected WorldEdit artifact/version for the configured minecraftVersion"
    doLast {
        val depNotation = "com.sk89q.worldedit:worldedit-fabric-mc${minecraftVersion}:$worldeditVersion"
        println("Requested dependency notation: $depNotation")
        val detached = configurations.detachedConfiguration(dependencies.create(depNotation))
        try {
            detached.resolve()
            val resolved = detached.resolvedConfiguration.resolvedArtifacts
            if (resolved.isEmpty()) {
                println("No artifacts were resolved for $depNotation (possible POM-only module).")
                println("Run locally for details:")
                println("  ./gradlew dependencyInsight --dependency com.sk89q.worldedit --configuration runtimeClasspath --info")
            } else {
                println("Resolved artifacts:")
                resolved.forEach {
                    println(" - ${it.moduleVersion.id} -> file=${it.file.name}")
                }
            }
        } catch (e: Exception) {
            println("Resolution failed: ${e.message}")
            println("Run with --info to see repository lookups and the selected version.")
            throw e
        }
    }
}

tasks.register("printVersion") {
    group = "help"
    description = "Print resolved project.version, artifactId, and resolved inputs"
    doLast {
        println("project.group = '$group'")
        println("project.version = '${project.version}'")
        println("artifactId (computed) = '$artifactIdFinal'")
        println("minecraftVersion (resolved) = '$minecraftVersion'")
        println("worldeditVersion(request) = '$worldeditVersion'")
    }
}

// Validate publish inputs early
tasks.register("validatePublish") {
    doLast {
        if (project.version.toString().isBlank()) {
            throw GradleException("Cannot publish: project.version is empty. Set mod_version in gradle.properties.")
        }
        if (artifactIdFinal != artifactIdFinal.lowercase(Locale.ROOT)) {
            throw GradleException("Computed artifactId must be lowercase for GitHub Packages. Computed: '$artifactIdFinal'")
        }
        println("Publishing coordinates: ${project.group}:$artifactIdFinal:${project.version}")
    }
}

tasks.withType(PublishToMavenRepository::class).configureEach {
    dependsOn("validatePublish")
}

// Publishing
val githubPackagesOwner = (findProperty("github_packages_owner") as String?) ?: "Erik-Donath"
val githubPackagesRepo = (findProperty("github_packages_repo") as String?) ?: "Lite2Edit"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = artifactIdFinal

            val remapJar = tasks.findByName("remapJar")
            val remapSourcesJar = tasks.findByName("remapSourcesJar")
            val sourcesJar = tasks.findByName("sourcesJar")
            val jarTask = tasks.findByName("jar")

            if (remapJar != null) {
                artifact(remapJar)
            } else if (jarTask != null) {
                from(components["java"])
            } else {
                try {
                    from(components["java"])
                } catch (_: Exception) {
                    // no java component
                }
            }

            when {
                remapSourcesJar != null -> artifact(remapSourcesJar)
                sourcesJar != null -> artifact(sourcesJar)
            }

            pom {
                name.set(artifactIdFinal)
                description.set("Lite2Edit - helper for Litematica schematics and WorldEdit")
                url.set("https://github.com/$githubPackagesOwner/$githubPackagesRepo")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("erikdonath")
                        name.set("Erik Donath")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/$githubPackagesOwner/$githubPackagesRepo")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: (findProperty("gpr.user") as String?)
                password = System.getenv("GITHUB_TOKEN") ?: (findProperty("gpr.key") as String?)
            }
        }
    }
}