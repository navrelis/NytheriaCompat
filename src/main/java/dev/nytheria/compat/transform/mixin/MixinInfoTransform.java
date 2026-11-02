/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.transform.mixin;

import net.lenni0451.classtransform.annotations.CInline;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CWrapCatch;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTargetAlreadyLoadedException;

import java.util.List;

@CTransformer(name = "org.spongepowered.asm.mixin.transformer.MixinInfo")
public class MixinInfoTransform {
    @CInline
    @CWrapCatch(value = "readDeclaredTargets(Lorg/spongepowered/asm/mixin/transformer/MixinInfo$MixinClassNode;Z)Ljava/util/List;")
    private List nytheriacompat$nightconfigfixes$disableNightConfigFixesLoadingTooEarly(MixinTargetAlreadyLoadedException exception) {
        if ("com.electronwill.nightconfig.core.io.ConfigParser".equals(exception.getTarget())
            && "ConfigParserFabricMixin".equals(exception.getMixin().getName())
            && "nightconfigfixes".equals(FabricUtil.getModId(exception.getMixin().getConfig()))
        ) {
            // Ignore the exception that throws when NightConfigFixes loads too early.
            return List.of();
        }

        throw exception;
    }
}
