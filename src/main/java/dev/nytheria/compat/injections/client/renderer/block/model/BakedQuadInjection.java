/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.block.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public interface BakedQuadInjection {
    default boolean hasAmbientOcclusion() {
        throw new IllegalStateException();
    }

    default void nytheriacompat$setAmbientOcclusion(boolean hasAo) {
        throw new IllegalStateException();
    }

    static BakedQuad withAo(int[] is, int i, Direction direction, TextureAtlasSprite textureAtlasSprite, boolean bl, boolean hasAo) {
        var quad = new BakedQuad(is, i, direction, textureAtlasSprite, bl);
        ((BakedQuadInjection) quad).nytheriacompat$setAmbientOcclusion(hasAo);

        return quad;
    }
}
