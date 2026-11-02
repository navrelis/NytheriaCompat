/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block.entity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.world.level.block.entity.BlockEntityTypeInjection;

import java.util.Collections;
import java.util.Set;

@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeInject<T extends BlockEntity> implements BlockEntityTypeInjection<T> {
    @Shadow @Final private Set<Block> validBlocks;

    @Override
    public Set<Block> getValidBlocks() {
        return Collections.unmodifiableSet(this.validBlocks);
    }
}
