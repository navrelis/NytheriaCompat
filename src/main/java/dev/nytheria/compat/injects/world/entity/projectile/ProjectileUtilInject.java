// TRACKED HASH: 0fb8232c7b5c6ca9e8e2fb7d17cfbd7bc1d6da7a
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.projectile;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.entity.projectile.ProjectileUtilInjection;
import dev.nytheria.compat.hook.Scope;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilInject implements ProjectileUtilInjection {
    @Definition(id = "entity", local = @Local(type = Entity.class, ordinal = 0))
    @Definition(id = "entity2", local = @Local(type = Entity.class, ordinal = 2))
    @Definition(id = "getRootVehicle", method = "Lnet/minecraft/world/entity/Entity;getRootVehicle()Lnet/minecraft/world/entity/Entity;")
    @Expression("entity2.getRootVehicle() == entity.getRootVehicle()")
    @ModifyExpressionValue(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean nytheriacompat$checkCanRiderInteract(boolean original, @Local(ordinal = 2) Entity entity) {
        return original && !(Scope.isNeoObject(entity) && entity.canRiderInteract());
    }

    @CreateStatic
    private static InteractionHand getWeaponHoldingHand(LivingEntity livingEntity, Predicate<Item> itemPredicate) {
        return ProjectileUtilInjection.getWeaponHoldingHand(livingEntity, itemPredicate);
    }
}