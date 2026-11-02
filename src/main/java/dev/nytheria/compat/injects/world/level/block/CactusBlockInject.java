/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import dev.nytheria.compat.hook.Scope;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(CactusBlock.class)
public abstract class CactusBlockInject extends Block {
    public CactusBlockInject(Properties properties) {
        super(properties);
    }

    @WrapOperation(method = "canSurvive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z", ordinal = 0))
    private boolean nytheriacompat$checkCanSustainPlant(BlockState instance, TagKey<Block> tagKey, Operation<Boolean> original, @Local(argsOnly = true) LevelReader level, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState state) {
        if (!Scope.isNeoObject((Object) instance.getBlock()))
            return original.call(instance, tagKey);

        var soilDecision = instance.canSustainPlant(level, pos, Direction.UP, state);
        if (!soilDecision.isDefault())
            return soilDecision.isTrue();

        return original.call(instance, tagKey);
    }
}
