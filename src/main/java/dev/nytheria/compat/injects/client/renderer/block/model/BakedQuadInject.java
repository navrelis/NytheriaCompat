// TRACKED HASH: f7771377b9b1ccc4e36e12d9261c92039fef8d18
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.block.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.client.renderer.block.model.BakedQuadInjection;

@Mixin(BakedQuad.class)
public class BakedQuadInject implements BakedQuadInjection {
    public BakedQuadInject(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade) {}

    @CreateInitializer
    public BakedQuadInject(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade, boolean hasAmbientOcclusion) {
        this(vertices, tintIndex, direction, sprite, shade);
        this.hasAmbientOcclusion = hasAmbientOcclusion;
    }

    private boolean hasAmbientOcclusion = true;

    @Override
    public void nytheriacompat$setAmbientOcclusion(boolean hasAo) {
        hasAmbientOcclusion = hasAo;
    }

    @Override
    public boolean hasAmbientOcclusion() {
        return hasAmbientOcclusion;
    }
}