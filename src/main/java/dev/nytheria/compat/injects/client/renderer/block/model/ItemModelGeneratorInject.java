/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.block.model;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.injections.client.renderer.block.model.BlockModelInjection;

@Mixin(ItemModelGenerator.class)
public abstract class ItemModelGeneratorInject {
    @ModifyReturnValue(method = "generateBlockModel", at = @At("RETURN"))
    private BlockModel nytheriacompat$copyCustomBlockModelData(BlockModel original, @Local(argsOnly = true) BlockModel source) {
        ((BlockModelInjection) original).nytheriacompat$getCustomData().copyFrom(((BlockModelInjection) source).nytheriacompat$getCustomData());
        ((BlockModelInjection) original).nytheriacompat$getCustomData().setGui3d(false);

        return original;
    }
}
