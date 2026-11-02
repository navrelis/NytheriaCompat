/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.levelgen.feature.treedecorators;


import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.event.level.AlterGroundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.AlterGroundDecorator;

@Mixin(AlterGroundDecorator.class)
public abstract class AlterGroundDecoratorInject {
    @Unique private final ThreadLocal<AlterGroundEvent.StateProvider> nytheriacompat$eventProvider = new ThreadLocal<>();

    @WrapOperation(method = "placeBlockAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;getState(Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState nytheriacompat$tryUseCustomEventProvider(BlockStateProvider instance, RandomSource randomSource, BlockPos pos, Operation<BlockState> original) {
        var eventProvider = this.nytheriacompat$eventProvider.get();
        if (eventProvider != null) {
            return eventProvider.getState(randomSource, pos);
        }

        return original.call(instance, randomSource, pos);
    }
}
