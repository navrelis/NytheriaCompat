/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.phys;

import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.phys.AABBInjection;

@Mixin(AABB.class)
public abstract class AABBInject implements AABBInjection {
    @Shadow @Final public double minX;
    @Shadow @Final public double minY;
    @Shadow @Final public double minZ;
    @Shadow @Final public double maxX;
    @Shadow @Final public double maxY;
    @Shadow @Final public double maxZ;
    @CreateStatic
    private static final AABB INFINITE = AABBInjection.INFINITE;

    @Override
    public boolean isInfinite() {
        return (Object) this == INFINITE || (Double.isInfinite(this.minX) && Double.isInfinite(this.minY) && Double.isInfinite(this.minZ)
            && Double.isInfinite(this.maxX) && Double.isInfinite(this.maxY) && Double.isInfinite(this.maxZ));
    }
}
