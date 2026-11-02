/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.resources.model;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.client.resources.model.ModelResourceLocationInjection;

@Mixin(ModelResourceLocation.class)
public abstract class ModelResourceLocationInject {
    @CreateStatic
    private static final String STANDALONE_VARIANT = ModelResourceLocationInjection.STANDALONE_VARIANT;

    @CreateStatic
    private static ModelResourceLocation standalone(ResourceLocation id) {
        return ModelResourceLocationInjection.standalone(id);
    }
}
