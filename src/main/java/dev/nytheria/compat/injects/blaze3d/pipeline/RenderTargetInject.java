/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.blaze3d.pipeline;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.github.fabricators_of_create.porting_lib.extensions.client.RenderTargetExtension;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.injections.blaze3d.pipeline.RenderTargetInjection;

@Mixin(RenderTarget.class)
public abstract class RenderTargetInject implements RenderTargetExtension, RenderTargetInjection {
    // Nytheria Compat: stencil stuff is handled by Porting Lib

    public void enableStencil() {
        this.port_lib$enableStencil();
    }

    public boolean isStencilEnabled() {
        return this.port_lib$isStencilEnabled();
    }
}
