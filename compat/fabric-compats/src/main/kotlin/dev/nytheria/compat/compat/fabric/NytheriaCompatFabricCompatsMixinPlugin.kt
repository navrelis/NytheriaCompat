/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.compat.fabric

import com.moulberry.mixinconstraints.MixinConstraints
import net.fabricmc.loader.api.FabricLoader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo
import dev.nytheria.compat.helpers.mixin.MixinExtensionHelper
import dev.nytheria.compat.loader.NytheriaCompatLoader

class NytheriaCompatFabricCompatsMixinPlugin : IMixinConfigPlugin {
    lateinit var mixinPackage: String

    override fun onLoad(mixinPackage: String) {
        this.mixinPackage = mixinPackage
    }

    override fun getRefMapperConfig(): String? {
        return null
    }

    override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean {
        val modId = mixinClassName.removePrefix("$mixinPackage.").replaceAfter(".", "").removeSuffix(".")

        if (modId == "sophisticatedcore") {
            return FabricLoader.getInstance().isModLoaded(modId) && !NytheriaCompatLoader.instance.hasMod(modId) && MixinConstraints.shouldApplyMixin(targetClassName, mixinClassName)
        }

        return MixinConstraints.shouldApplyMixin(targetClassName, mixinClassName)
    }

    override fun acceptTargets(
        myTargets: Set<String?>?,
        otherTargets: Set<String?>?
    ) {
    }

    override fun getMixins(): List<String?>? {
        return null
    }

    override fun preApply(
        targetClassName: String?,
        targetClass: ClassNode?,
        mixinClassName: String?,
        mixinInfo: IMixinInfo?
    ) {
        MixinExtensionHelper.preApply(targetClassName, targetClass, mixinClassName, mixinInfo)
    }

    override fun postApply(
        targetClassName: String?,
        targetClass: ClassNode?,
        mixinClassName: String?,
        mixinInfo: IMixinInfo?
    ) {
        MixinExtensionHelper.postApply(targetClassName, targetClass, mixinClassName, mixinInfo)
    }
}
