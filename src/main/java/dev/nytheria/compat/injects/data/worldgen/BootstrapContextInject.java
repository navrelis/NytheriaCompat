// TRACKED HASH: 50ccda2541c46785a0be46cf552b476164633abf
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.data.worldgen;

import net.minecraft.data.worldgen.BootstrapContext;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.injections.data.worldgen.BootstrapContextInjection;

@Mixin(BootstrapContext.class)
public interface BootstrapContextInject extends BootstrapContextInjection {
}