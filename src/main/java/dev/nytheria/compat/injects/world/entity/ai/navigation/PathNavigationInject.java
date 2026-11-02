/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.ai.navigation;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.nytheria.compat.hook.Scope;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge PathNavigation.java.patch (followThePath, "Forge: Fix MC-94054"):
 * <pre>
 * double d0 = Math.abs(mob.getX() - (vec3i.getX() + ((int)(mob.getBbWidth() + 1)) / 2D));
 * double d2 = Math.abs(mob.getZ() - (vec3i.getZ() + ((int)(mob.getBbWidth() + 1)) / 2D));
 * boolean flag = d0 <= maxDistanceToWaypoint && d2 <= maxDistanceToWaypoint && d1 < 1.0D;
 * </pre>
 * The node offset must match {@code Path#getEntityPosAtNode}, which truncates {@code width + 1} to an int.
 */
@Mixin(PathNavigation.class)
public abstract class PathNavigationInject {
    @Shadow @Final protected Mob mob;

    @ModifyExpressionValue(method = "followThePath", at = @At(value = "CONSTANT", args = "doubleValue=0.5"))
    private double nytheriacompat$fixMC94054(double original) {
        // The int cast matters: without it a mob 0.75..1.0 wide (cow, pig, sheep, many modded mobs) is
        // steered to node + 0.5 by getEntityPosAtNode but only counted as arrived at node + ~0.95,
        // exactly maxDistanceToWaypoint away, so it never advances past its first node and stands still.
        return Scope.isNeoObject(this.mob) ? ((int) (this.mob.getBbWidth() + 1)) / 2D : original;
    }

    // NeoForge: "d <= maxDistanceToWaypoint" instead of "<". Raising the float by one ulp before the
    // f2d widening makes the vanilla "<" accept d == maxDistanceToWaypoint.
    @ModifyExpressionValue(method = "followThePath", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;maxDistanceToWaypoint:F",
            opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float nytheriacompat$fixMC94054LessOrEqual(float original) {
        return Scope.isNeoObject(this.mob) ? Math.nextUp(original) : original;
    }
}
