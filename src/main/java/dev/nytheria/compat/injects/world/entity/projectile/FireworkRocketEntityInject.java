/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityInject extends Projectile {
    public FireworkRocketEntityInject(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Intrinsic
    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.MISS || !EventHooks.onProjectileImpact(this, result)) {
            super.onHit(result);
        }
    }
}
