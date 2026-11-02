// TRACKED HASH: 62c27db64352c1bb41803757381fc0825f5315fe
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.texture;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.client.renderer.texture.TextureAtlasInjection;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasInject implements TextureAtlasInjection {
    @Shadow private Map<ResourceLocation, TextureAtlasSprite> texturesByName;

    @Override
    public Map<ResourceLocation, TextureAtlasSprite> getTextures() {
        return Collections.unmodifiableMap(texturesByName);
    }
}