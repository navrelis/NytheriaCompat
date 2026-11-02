// TRACKED HASH: 47476edb59fcfcf846692ab9ec3986cad3c5d6c4
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.injections.world.item.SpawnEggItemInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@Mixin(SpawnEggItem.class)
public class SpawnEggItemInject implements SpawnEggItemInjection {
    @Shadow @Final private EntityType<?> defaultType;

    @WrapOperation(method = "getType", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/SpawnEggItem;defaultType:Lnet/minecraft/world/entity/EntityType;", opcode = 0))
    private EntityType<?> nytheriacompat$useForgeDefaultType(SpawnEggItem instance, Operation<EntityType<?>> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(this.getClass(), SpawnEggItem.class, "getDefaultType"))
            return this.getDefaultType();

        return original.call(instance);
    }

    @ModifyReturnValue(method = "getType", at = @At("RETURN"))
    private EntityType<?> nytheriacompat$returnForgeDefaultType(EntityType<?> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(this.getClass(), SpawnEggItem.class, "getDefaultType"))
            return this.getDefaultType();

        return original;
    }

    @ModifyReceiver(method = "requiredFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;requiredFeatures()Lnet/minecraft/world/flag/FeatureFlagSet;"))
    private EntityType<?> nytheriacompat$useForgeDefaultTypeForFeatures(EntityType<?> instance) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(this.getClass(), SpawnEggItem.class, "getDefaultType"))
            return this.getDefaultType();

        return instance;
    }

    @Override
    public EntityType<?> getDefaultType() {
        return this.defaultType;
    }
}