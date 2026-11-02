/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

import net.fabricmc.loom.task.RemapJarTask
import dev.nytheria.compat.gradle.loom.NytheriaCompatLoomPlugin

apply<NytheriaCompatLoomPlugin>()

base {
    archivesName.set("NytheriaCompat-Fabric-Mod-Compats")
}

version = property("mod_version") as String

dependencies {
    modCompileOnly("maven.modrinth:9jxwkYQL:${property("sophisticatedcore_version")}") // Sophisticated Core (Unofficial Fabric port); the slug no longer resolves on Modrinth Maven, use the project id
}

tasks {
    processResources {
        val properties = mutableMapOf(
            "version" to project.version,
            "loader_version" to project.property("loader_min_version"),
            "fabric_version" to project.property("fabric_version"),
            "minecraft_version" to project.property("minecraft_version"),
            "fabric_kotlin_version" to project.property("fabric_kotlin_version")
        )

        for ((key, value) in properties) {
            inputs.property(key, value)
        }

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(properties)
        }
    }
}

// Add compat layer to nested JARs in base Nytheria Compat project.
rootProject.tasks.getByName<RemapJarTask>("remapJar").nestedJars.from(project.tasks.getByName("remapJar"))
