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

val targetJavaVersion = 17
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
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

    // WorldEdit (runtime or compileOnly depending on your use)
    modImplementation("com.sk89q.worldedit:worldedit-fabric-mc$minecraftVersion:$worldeditVersion")

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

// Robust GitHub Packages target derived from properties or environment
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = base.archivesName.get()
            version = project.version.toString()

            pom {
                name.set(base.archivesName.get())
                description.set("Lite2Edit Fabric mod artifacts for $minecraftVersion")
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

            // Prefer gradle.properties keys, fallback to env from Actions
            val ghOwnerProp = (findProperty("github.owner") as String?)
            val ghRepoProp = (findProperty("github.repo") as String?)

            val ghRepoEnvFull = System.getenv("GITHUB_REPOSITORY") // e.g. Owner/Repo
            val ghOwnerEnv = System.getenv("GITHUB_OWNER")
            val ghOwner = ghOwnerProp ?: ghOwnerEnv ?: ghRepoEnvFull?.substringBefore("/")
            val ghRepo = ghRepoProp ?: ghRepoEnvFull?.substringAfter("/")

            require(!ghOwner.isNullOrBlank()) { "GitHub owner not resolved. Set gradle property github.owner or env GITHUB_OWNER/GITHUB_REPOSITORY." }
            require(!ghRepo.isNullOrBlank()) { "GitHub repo not resolved. Set gradle property github.repo or env GITHUB_REPOSITORY." }

            url = uri("https://maven.pkg.github.com/$ghOwner/$ghRepo")

            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: (findProperty("gpr.user") as String?)
                password = System.getenv("GITHUB_TOKEN") ?: (findProperty("gpr.key") as String?)
            }
        }
    }
}
