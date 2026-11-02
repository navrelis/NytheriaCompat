/*
 * Changed by the Nytheria Compat project between 2026-09-30 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "FabricMC"
        }
        maven("https://maven.quiltmc.org/repository/release/") {
            name = "QuiltMC"
        }
        maven("https://dl.bintray.com/brambolt/public")
        maven("https://mvn.devos.one/releases") {
            name = "devOS Releases"
        }
        maven("https://mvn.devos.one/snapshots") {
            name = "devOS Releases"
        }
        maven("https://repo.sleeping.town/")
        maven("https://maven.neoforged.net/releases")
        mavenCentral()
        gradlePluginPortal()
    }

    val fabric_kotlin_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version
                fabric_kotlin_version
                    .split("+kotlin.")[1] // Grabs the sentence after `+kotlin.`
                    .split("+")[0] // Ensures sentences like `+build.1` are ignored
    }
}

rootProject.name = "NytheriaCompat" // the jar is NytheriaCompat-<version>.jar

includeBuild("build-logic")

file("compat").listFiles { file -> file.isDirectory && file.name != ".gradle" && file.name != "build" }.forEach {
    include(":compat:${it.name}")
    project(":compat:${it.name}").apply {
        this.projectDir = it
    }
}

include(":loader:cichlid")
include(":loader:fabric")
//include(":loader:quilt") // TODO: Quilt Loom is broken on 1.21.1 from the looks.
