// TRACKED HASH: b90940f2b56f4abadcf439bc0d7b45a206633f68
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.color.item;

import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;

@Mixin(ItemColors.class)
public abstract class ItemColorsInject {
    @Inject(at = @At("RETURN"), method = "createDefault")
    private static void nytheriacompat$initForgeItemColors(BlockColors blockColors, CallbackInfoReturnable<ItemColors> cir) {
        ClientHooks.onItemColorsInit(cir.getReturnValue(), blockColors);
    }

    // Nytheria Compat TODO: is this still needed?
}
