/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.workarounds.avoid_creative_tab_duplicates;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.NytheriaCompat;

@Mixin(CreativeModeTab.ItemDisplayBuilder.class)
public abstract class CreativeModeTabItemDisplayBuilderMixin {
    @Shadow @Final private CreativeModeTab tab;

    @Definition(id = "IllegalStateException", type = IllegalStateException.class)
    @Expression("throw new IllegalStateException(?)")
    @Inject(method = "accept", at = @At("MIXINEXTRAS:EXPRESSION"), cancellable = true)
    private void nytheriacompat$avoidCrashingStackAdd(ItemStack stack, CreativeModeTab.TabVisibility tabVisibility, CallbackInfo ci) {
        // because Neo actually ignores this unintentionally, fun fact.
        // see: https://github.com/neoforged/NeoForge/issues/3431
        ci.cancel();
        NytheriaCompat.Companion.getLogger().error("Accidentally adding the same item stack twice " + stack.getDisplayName().getString() + " to a Creative Mode Tab: " + this.tab.getDisplayName().getString());
        NytheriaCompat.Companion.getLogger().error("Nytheria Compat is working around this, because NeoForge mods actually rely on this behaviour.");
    }
}
