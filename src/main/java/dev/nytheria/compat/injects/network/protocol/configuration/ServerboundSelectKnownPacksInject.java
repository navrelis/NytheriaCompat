/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.network.protocol.configuration;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerboundSelectKnownPacks.class, priority = 1250)
public abstract class ServerboundSelectKnownPacksInject {
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "CONSTANT", args = "intValue=64"))
    private static int nytheriacompat$increaseKnownPacksValue(int original) {
        if (original > 64) // Nytheria Compat: prioritize other mixins
            return original;

        return 1024;
    }
}
