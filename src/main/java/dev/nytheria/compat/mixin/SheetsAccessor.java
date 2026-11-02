/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.WoodType;

@Mixin(Sheets.class)
public interface SheetsAccessor {
    @Invoker
    static Material callCreateSignMaterial(WoodType woodType) {
        throw new UnsupportedOperationException();
    }

    @Invoker
    static Material callCreateHangingSignMaterial(WoodType woodType) {
        throw new UnsupportedOperationException();
    }

    @Invoker
    static Material callCreateDecoratedPotMaterial(ResourceLocation assetId) {
        throw new UnsupportedOperationException();
    }
}
