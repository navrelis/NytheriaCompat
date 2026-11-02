/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.gradle.loom

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginAware

class NytheriaCompatLoomPlugin : Plugin<PluginAware> {
    override fun apply(target: PluginAware) {
        if (target is Project)
            applyProject(target)
    }

    fun applyProject(project: Project) {
        project.logger.lifecycle("Applying Nytheria Compat Jar transformers")

        val loomExtension = project.extensions.getByName("loom") as LoomGradleExtensionAPI

//        loomExtension.addMinecraftJarProcessor(AccessTransformerProcessor::class.java)
    }
}
