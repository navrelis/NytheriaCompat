/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.material.FluidState
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
import net.neoforged.neoforge.client.textures.FluidSpriteCache

class NeoForgeFluidRenderHandler : FluidRenderHandler {
    override fun getFluidSprites(
        view: BlockAndTintGetter?,
        pos: BlockPos?,
        state: FluidState?
    ): Array<TextureAtlasSprite?> {
        return FluidSpriteCache.getFluidSprites(view, pos, state).filterNotNull().toTypedArray()
    }

    override fun getFluidColor(view: BlockAndTintGetter?, pos: BlockPos?, state: FluidState?): Int {
        val extensions = IClientFluidTypeExtensions.of(state)
        if (view == null || pos == null) // Some mods rely on this info really early, but we can't reasonably get the state at that point of time.
            return extensions.tintColor

        return extensions.getTintColor(state, view, pos)
    }
}
