/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.storage.loot;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(LootParams.class)
public abstract class LootParamsInject {
    @Mixin(LootParams.Builder.class)
    public static abstract class BuilderInject {
        @WrapOperation(method = "create", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"))
        private boolean nytheriacompat$allowCustomLootParams(Set instance, Operation<Boolean> original) {
            return original.call(instance) || (instance.size() == 1 && instance.contains(LootContextParams.ATTACKING_ENTITY));
        }
    }
}
