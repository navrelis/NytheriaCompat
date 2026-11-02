/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions

class FabricFluidTypeExtensions(private val fluid: Fluid) : IClientFluidTypeExtensions {
    private val renderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid)
    private val isInvalid: Boolean
        get() {
            return renderHandler is NeoForgeFluidRenderHandler
        }

    override fun getStillTexture(): ResourceLocation? {
        if (isInvalid) return null
        return renderHandler?.getFluidSprites(null, null, fluid.defaultFluidState())[0]?.contents()?.name()
    }

    override fun getFlowingTexture(): ResourceLocation? {
        if (isInvalid) return null
        return renderHandler?.getFluidSprites(null, null, fluid.defaultFluidState())[1]?.contents()?.name()
    }

    override fun getOverlayTexture(): ResourceLocation? {
        if (isInvalid) return null
        return renderHandler?.getFluidSprites(null, null, fluid.defaultFluidState())?.getOrNull(2)?.contents()?.name()
    }

    override fun getStillTexture(state: FluidState, getter: BlockAndTintGetter?, pos: BlockPos?): ResourceLocation? {
        if (isInvalid) return null
        return renderHandler?.getFluidSprites(getter, pos, state)[0]?.contents()?.name()
    }

    override fun getFlowingTexture(state: FluidState?, getter: BlockAndTintGetter?, pos: BlockPos?): ResourceLocation? {
        if (isInvalid) return null
        return renderHandler?.getFluidSprites(getter, pos, state)[1]?.contents()?.name()
    }

    override fun getOverlayTexture(state: FluidState?, getter: BlockAndTintGetter?, pos: BlockPos?): ResourceLocation? {
        if (isInvalid) return null
        return renderHandler?.getFluidSprites(getter, pos, state)?.getOrNull(2)?.contents()?.name()
    }

    override fun getTintColor(): Int {
        if (isInvalid) return -1
        return renderHandler?.getFluidColor(null, null, fluid.defaultFluidState()) ?: -1
    }

    override fun getTintColor(state: FluidState?, getter: BlockAndTintGetter?, pos: BlockPos?): Int {
        if (isInvalid) return -1
        return renderHandler?.getFluidColor(getter, pos, state) ?: -1
    }
}
