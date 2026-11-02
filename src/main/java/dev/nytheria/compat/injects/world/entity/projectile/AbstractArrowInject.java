/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.projectile;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowInject extends Projectile {
    // Non-NONE sentinel: vanilla's "deflection != NONE -> break" then leaves the loop like NeoForge's break on a cancelled impact event
    @Unique private static final ProjectileDeflection NYTHERIACOMPAT$BREAK = (projectile, entity, random) -> {};

    public AbstractArrowInject(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z", ordinal = 0))
    private boolean nytheriacompat$clearFireIfFluidExtinguishes(boolean original) {
        var entity = this;

        return original || (this.isInFluidType() && this.isInFluidType((fluidType, height) -> entity.canFluidExtinguish(fluidType)));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;hitTargetOrDeflectSelf(Lnet/minecraft/world/phys/HitResult;)Lnet/minecraft/world/entity/projectile/ProjectileDeflection;"))
    private ProjectileDeflection nytheriacompat$handleImpact(AbstractArrow instance, HitResult hitResult, Operation<ProjectileDeflection> original) {
        // The level clip returns a non-null MISS every tick; NeoForge never lets it reach onHit
        if (hitResult.getType() == HitResult.Type.MISS) {
            return ProjectileDeflection.NONE;
        }

        if (EventHooks.onProjectileImpact(this, hitResult)) {
            return NYTHERIACOMPAT$BREAK;
        }

        return original.call(instance, hitResult);
    }
}
