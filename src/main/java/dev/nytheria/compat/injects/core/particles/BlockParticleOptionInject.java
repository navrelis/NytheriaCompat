// TRACKED HASH: 2a097bd56b2bffdbb2ca713b3f2bf5c5f738f883
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.core.particles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.injections.core.particles.BlockParticleOptionInjection;

@Mixin(BlockParticleOption.class)
public class BlockParticleOptionInject implements BlockParticleOptionInjection {
    @Unique
    private BlockPos pos;

    @Override
    public BlockParticleOption setPos(BlockPos pos) {
        this.pos = pos;
        return (BlockParticleOption) (Object) this;
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }
}