/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler
import net.neoforged.neoforge.fluids.FluidType

object FluidHandlerWorkaround {
    private val neoForgeFluidRenderHandler = NeoForgeFluidRenderHandler()

    fun getFluidRenderHandler(fluidType: FluidType): FluidRenderHandler {
        return neoForgeFluidRenderHandler
    }
}
