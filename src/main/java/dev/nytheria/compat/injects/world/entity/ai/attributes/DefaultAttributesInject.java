// TRACKED HASH: 04ed1ed8c5f75415ffb7509faccf5a3e9d757b25
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.ai.attributes;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(DefaultAttributes.class)
public class DefaultAttributesInject {
    @WrapOperation(at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), method = "getSupplier")
    private static <K, V> V nytheriacompat$useForgeSupplier(Map<K, V> instance, Object o, Operation<V> original, @Local(argsOnly = true) EntityType<? extends LivingEntity> entityType) {
        var supplier = CommonHooks.getAttributesView().get(entityType);
        return supplier != null ? (V) supplier : original.call(instance, o);
    }

    @ModifyReturnValue(at = @At("RETURN"), method = "hasSupplier")
    private static boolean nytheriacompat$hasForgeSupplier(boolean original, @Local(argsOnly = true) EntityType<?> type) {
        return original || CommonHooks.getAttributesView().containsKey(type);
    }
}