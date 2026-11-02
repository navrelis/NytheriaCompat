/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import com.google.common.collect.BiMap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.common.DataMapHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.WeatheringCopper;

@Mixin(WeatheringCopper.class)
public interface WeatheringCopperInject extends ChangeOverTimeBlock<WeatheringCopper.WeatherState> {

    @WrapOperation(method = {
        "getNext(Lnet/minecraft/world/level/block/Block;)Ljava/util/Optional;"
    }, at = @At(value = "INVOKE", target = "Lcom/google/common/collect/BiMap;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static <T, U> U nytheriacompat$tryGetNextOxidizedStage(BiMap<T, U> instance, T o, Operation<U> original) {
        if (o instanceof Block block) {
            var value = DataMapHooks.getNextOxidizedStage(block);

            if (value != null) {
                return (U) value;
            }
        }

        return original.call(instance, o);
    }
}
