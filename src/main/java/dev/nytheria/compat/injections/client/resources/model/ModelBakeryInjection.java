/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.resources.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import dev.nytheria.compat.processor.FabricInjectedInterface;

import java.util.function.Function;

public interface ModelBakeryInjection {
    @FabricInjectedInterface(target = "net.minecraft.client.resources.model.ModelBakery$ModelBakerImpl")
    interface ModelBakerImplInjection {
        BakedModel bake(ResourceLocation loc, ModelState state, Function<Material, TextureAtlasSprite> sprites);

        Function<Material, TextureAtlasSprite> getModelTextureGetter();
    }
}
