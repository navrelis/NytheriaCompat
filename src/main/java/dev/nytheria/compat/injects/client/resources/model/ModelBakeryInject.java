// TRACKED HASH: 9e81b9914e73796e61fbdf90eb1c997d3da3bf81
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.resources.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.client.resources.model.ModelBakeryInjection;

import java.util.function.Function;

@Mixin(ModelBakery.class)
public abstract class ModelBakeryInject implements ModelBakeryInjection {

    @Mixin(targets = "net.minecraft.client.resources.model.ModelBakery$ModelBakerImpl")
    public static abstract class ModelBakerImplInject implements ModelBaker, ModelBakerImplInjection {
        @Shadow @Final @Mutable private Function<Material, TextureAtlasSprite> modelTextureGetter;
        @Shadow public abstract BakedModel bake(ResourceLocation location, ModelState transform);
        @Shadow @Final private ModelBakery field_40571;
        @Shadow @Nullable abstract BakedModel bakeUncached(UnbakedModel unbakedModel, ModelState modelState);

        @Override
        public Function<Material, TextureAtlasSprite> getModelTextureGetter() {
            return this.modelTextureGetter;
        }

        @Override
        public BakedModel bake(ResourceLocation loc, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
            var original = this.modelTextureGetter;
            this.modelTextureGetter = sprites;
            var result = this.bake(loc, state);
            this.modelTextureGetter = original;

            return result;
        }

        @Override
        public @Nullable BakedModel bakeUncached(UnbakedModel model, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
            var original = this.modelTextureGetter;
            this.modelTextureGetter = sprites;
            var result = this.bakeUncached(model, state);
            this.modelTextureGetter = original;

            return result;
        }
    }
}