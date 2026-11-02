/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

subprojects {
    apply(plugin = "fabric-loom")

    dependencies {
        implementation(project(rootProject.path, configuration = "namedElements"))

        api("net.neoforged:bus:${property("eventbus_version")}") {
            exclude("org.ow2.asm")
        }
    }

    // Each module's fabric.mod.json declares "icon": "icon.png"; ship Nytheria Compat's icon at that path so Mod Menu shows it.
    tasks.named<ProcessResources>("processResources") {
        from(rootProject.file("src/main/resources/assets/nytheria_compat/icon.png"))
    }
}