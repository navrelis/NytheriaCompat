// TRACKED HASH: ee2d4b36c902294bee93d8bff904182c9ba84f31
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.block.model;

import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;

import java.util.List;
import java.util.function.Function;

@Mixin(ItemOverrides.class)
public class ItemOverridesInject {
    @Unique private Function<Material, TextureAtlasSprite> spriteGetter;

    public ItemOverridesInject(ModelBaker baker, UnbakedModel model, List<ItemOverride> overrides) {}

    // TODO: figure out how to make ItemOverrides take spriteGetter
    @CreateInitializer
    public ItemOverridesInject(ModelBaker baker, UnbakedModel model, List<ItemOverride> overrides, Function<Material, TextureAtlasSprite> spriteGetter) {
        this(baker, model, overrides);
    }
}