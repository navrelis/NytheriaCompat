/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.texture.atlas;

import net.neoforged.neoforge.client.textures.SpriteContentsConstructor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

@Mixin(SpriteResourceLoader.class)
public interface SpriteResourceLoaderInject {
    @Shadow @Nullable SpriteContents loadSprite(ResourceLocation resourceLocation, Resource resource);

    // Nytheria Compat TODO: how in the fuck do we do this one?

    @Nullable
    default SpriteContents loadSprite(ResourceLocation id, Resource resource, SpriteContentsConstructor constructor) {
        return loadSprite(id, resource);
    }
}
