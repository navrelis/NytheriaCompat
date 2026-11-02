/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.level.Level;

@Mixin(Zombie.class)
public abstract class ZombieInject extends Monster {

    protected ZombieInject(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @WrapOperation(method = "convertToZombieType", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Zombie;setCanBreakDoors(Z)V")) // only way i'm able to do this
    private void nytheriacompat$callLivingConvert(Zombie instance, boolean canBreakDoors, Operation<Void> original) {
        original.call(instance, canBreakDoors);
        EventHooks.onLivingConvert(this, instance);
    }

    @Inject(method = "killedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/ZombieVillager;setVillagerXp(I)V", shift = At.Shift.AFTER))
    private void nytheriacompat$callLivingConvertVillager(ServerLevel level, LivingEntity entity, CallbackInfoReturnable<Boolean> cir, @Local ZombieVillager zombieVillager) {
        EventHooks.onLivingConvert(entity, zombieVillager);
    }
}
