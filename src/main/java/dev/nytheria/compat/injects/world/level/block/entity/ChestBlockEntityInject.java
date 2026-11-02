/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block.entity;

import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityInject extends RandomizableContainerBlockEntity {
    protected ChestBlockEntityInject(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Intrinsic
    public void setBlockState(BlockState state) {
        var oldState = this.getBlockState();
        super.setBlockState(state);
        if ((oldState.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING)) || (oldState.getValue(ChestBlock.TYPE) != state.getValue(ChestBlock.TYPE))) {
            this.invalidateCapabilities();
        }
    }
}
