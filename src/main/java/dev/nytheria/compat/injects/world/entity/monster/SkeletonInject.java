/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

@Mixin(Skeleton.class)
public abstract class SkeletonInject extends AbstractSkeleton {

    protected SkeletonInject(EntityType<? extends AbstractSkeleton> entityType, Level level) {
        super(entityType, level);
    }

    @WrapOperation(method = "doFreezeConversion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Skeleton;convertTo(Lnet/minecraft/world/entity/EntityType;Z)Lnet/minecraft/world/entity/Mob;"))
    private Mob nytheriacompat$callLivingConvertEvent(Skeleton instance, EntityType entityType, boolean b, Operation<Mob> original) {
        Mob stray = original.call(instance, entityType, b);

        if (stray != null)
            EventHooks.onLivingConvert(this, stray);

        return stray;
    }
}
