import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.20"
    id("fabric-loom") version "1.11.8"
    id("maven-publish")
}

//
// Version / group handling
// - Use mod_version / maven_group from gradle.properties by default (existing), but allow
//   overriding the published version via -PreleaseVersion=... or the RELEASE_VERSION env var
//   (the publish workflow will set RELEASE_VERSION from the tag).
//
val defaultModVersion = (findProperty("mod_version") as String?) ?: "0.1.0"
val defaultGroup = (findProperty("maven_group") as String?) ?: "com.erikdonath"

// releaseVersion property takes precedence (use -PreleaseVersion=... from workflow), then env, then default
val releaseVersionProp = (findProperty("releaseVersion") as String?)
val releaseVersionEnv = System.getenv("RELEASE_VERSION")
version = releaseVersionProp ?: releaseVersionEnv ?: defaultModVersion
group = (findProperty("maven_group") as String?) ?: defaultGroup

base {
    archivesName.set((findProperty("archives_base_name") as String?) ?: project.name)
}

val targetJavaVersion = 17
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.enginehub.org/repo/") }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    modImplementation("com.sk89q.worldedit:worldedit-fabric-mc${project.property("minecraft_version")}:${project.property("worldedit_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    inputs.property("kotlin_loader_version", project.property("kotlin_loader_version"))
    inputs.property("worldedit_version", project.property("worldedit_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version"),
            "kotlin_loader_version" to project.property("kotlin_loader_version"),
            "worldedit_version" to project.property("worldedit_version")
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

// Publishing configuration
// - Publish the remapped mod jar if Loom provides remapJar/remapSourcesJar; otherwise publish the standard jar/sourcesJar.
// - Credentials are read from env (GITHUB_ACTOR/GITHUB_TOKEN) or gradle properties (gpr.user/gpr.key).
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
                // publish remapped jar (recommended for Fabric mods)
                artifact(remapJar)
            } else if (jarTask != null) {
                from(components["java"])
            } else {
                // fallback: try to publish whatever "java" component provides
                try {
                    from(components["java"])
                } catch (_: Exception) {
                    // nothing to do; user can customize if their layout is different
                }
            }

            // publish a sources jar if available
            when {
                remapSourcesJar != null -> artifact(remapSourcesJar)
                sourcesJar != null -> artifact(sourcesJar)
                else -> {
                    // withSourcesJar() above likely created 'sourcesJar'; if not present, skip silently
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