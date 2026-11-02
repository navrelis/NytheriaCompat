/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.Map;
import java.util.Set;

public interface TextureAtlasInjection {
    default Map<ResourceLocation, TextureAtlasSprite> getTextures() {
        throw NytheriaCompatHelper.createMixinException(TextureAtlasInjection.class, "getTextures");
    }
}
