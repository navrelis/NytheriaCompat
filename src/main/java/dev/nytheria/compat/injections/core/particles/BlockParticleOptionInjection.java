/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.core.particles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@FabricInjectedInterface(BlockParticleOption.class)
public interface BlockParticleOptionInjection {
    default BlockParticleOption setPos(BlockPos pos) {
        throw NytheriaCompatHelper.createMixinException(BlockParticleOptionInjection.class, "setPos");
    }

    default BlockPos getPos() {
        throw NytheriaCompatHelper.createMixinException(BlockParticleOptionInjection.class, "getPos");
    }
}
