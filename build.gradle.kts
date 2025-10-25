import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.GradleException

plugins {
    kotlin("jvm") version "2.2.20"
    id("fabric-loom") version "1.11.8"
    id("maven-publish")
}

//
// Version / group handling
// - Use -PreleaseVersion=... (Gradle property) first
// - Then READ environment variable RELEASE_VERSION (workflow sets this)
// - Then fall back to mod_version from gradle.properties
//
val defaultModVersion = (findProperty("mod_version") as String?) ?: "0.1.0"
val defaultGroup = (findProperty("maven_group") as String?) ?: "com.erikdonath"

// read -PreleaseVersion if provided
val releaseVersionProp = if (project.hasProperty("releaseVersion")) project.property("releaseVersion") as String else null
val releaseVersionEnv = System.getenv("RELEASE_VERSION")

version = releaseVersionProp?.takeIf { it.isNotBlank() } ?: releaseVersionEnv?.takeIf { it.isNotBlank() } ?: defaultModVersion
group = (findProperty("maven_group") as String?) ?: defaultGroup

base {
    archivesName.set((findProperty("archives_base_name") as String?) ?: project.name)
}

// Target Java version for compilation and Loom toolchain
val targetJavaVersion = 17
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

// Read Minecraft version and WorldEdit version in a flexible way:
// 1) -PminecraftVersion / -PworldeditVersion (Gradle properties passed on CLI)
// 2) Environment variables MINECRAFT_VERSION / WORLDEDIT_VERSION (CI)
// 3) gradle.properties defaults (minecraft_version / worldedit_version)
// 4) sensible fallback
val defaultMinecraft = (findProperty("minecraft_version") as String?) ?: "1.20.4"
val minecraftVersionProp = if (project.hasProperty("minecraftVersion")) project.property("minecraftVersion") as String else null
val minecraftVersionEnv = System.getenv("MINECRAFT_VERSION")
val minecraftVersion = (minecraftVersionProp ?: minecraftVersionEnv ?: defaultMinecraft).trim()

// For WorldEdit pick version from -P or env or gradle.properties; fallback to dynamic "7.+"
// Using "7.+" allows Gradle to pick the latest 7.x WorldEdit compatible artifact.
// You can override with -PworldeditVersion or WORLDEDIT_VERSION env var if needed.
val defaultWorldEdit = (findProperty("worldedit_version") as String?) ?: "7.+"
val worldeditVersionProp = if (project.hasProperty("worldeditVersion")) project.property("worldeditVersion") as String else null
val worldeditVersionEnv = System.getenv("WORLDEDIT_VERSION")
val worldeditVersion = (worldeditVersionProp ?: worldeditVersionEnv ?: defaultWorldEdit).trim()

repositories {
    mavenCentral()
    maven { url = uri("https://maven.enginehub.org/repo/") }
}

dependencies {
    // To change other versions, update gradle.properties or pass -P overrides.
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    // WorldEdit: artifact name includes the Minecraft version (same pattern as before).
    // Version is dynamic by default (7.+), or can be overridden by -PworldeditVersion or env var WORLDEDIT_VERSION.
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
        rename { "${it}_${project.base.archivesName}" }
    }
}

// small helper task so CI / humans can print version before publishing (helps debugging)
tasks.register("printVersion") {
    group = "help"
    description = "Print resolved project.version and resolved build inputs"
    doLast {
        println("Resolved project.version = '${project.version}'")
        println("Resolved minecraftVersion = '$minecraftVersion'")
        println("Resolved worldeditVersion = '$worldeditVersion'")
        println("Resolved group = '$group'")
        println("Resolved archivesBaseName = '${project.base.archivesName.get()}'")
    }
}

// Publishing configuration (unchanged except to use the same environment/property hooks for credentials)
val githubPackagesOwner = (findProperty("github_packages_owner") as String?) ?: "Erik-Donath"
val githubPackagesRepo = (findProperty("github_packages_repo") as String?) ?: "Lite2Edit"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = (findProperty("archives_base_name") as String?) ?: project.name

            // Prefer Loom remapped outputs if available
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
                    // no java component: nothing to do
                }
            }

            when {
                remapSourcesJar != null -> artifact(remapSourcesJar)
                sourcesJar != null -> artifact(sourcesJar)
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