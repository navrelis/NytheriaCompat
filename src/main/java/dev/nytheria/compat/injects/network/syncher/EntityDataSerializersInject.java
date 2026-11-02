// TRACKED HASH: 381478d70082864904d99c0e2af6d7b72e1615b7
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.network.syncher;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;

@Mixin(EntityDataSerializers.class)
public class EntityDataSerializersInject {
    @Shadow @Final private static CrudeIncrementalIntIdentityHashBiMap<EntityDataSerializer<?>> SERIALIZERS;

    @WrapOperation(method = "getSerializer", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CrudeIncrementalIntIdentityHashBiMap;byId(I)Ljava/lang/Object;"))
    private static <V> V nytheriacompat$getSerializerFromForge(CrudeIncrementalIntIdentityHashBiMap<V> instance, int id, Operation<V> original) {
        return (V) CommonHooks.nytheriacompat$getSerializer(id, () -> (EntityDataSerializer<?>) original.call(instance, id));
    }

    @WrapOperation(method = "getSerializedId", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CrudeIncrementalIntIdentityHashBiMap;getId(Ljava/lang/Object;)I"))
    private static <V> int nytheriacompat$getSerializerIdFromForge(CrudeIncrementalIntIdentityHashBiMap<V> instance, V value, Operation<Integer> original) {
        return CommonHooks.nytheriacompat$getSerializerId((EntityDataSerializer<?>) value, () -> original.call(instance, value));
    }
}
