/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.block;

import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockModelShaperInjection {
    default TextureAtlasSprite getTexture(BlockState state, Level level, BlockPos pos) {
        throw NytheriaCompatHelper.createMixinException(BlockModelShaperInjection.class, "getTexture");
    }
}
