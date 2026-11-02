// TRACKED HASH: 53d4aac2577bd786b144c351ddc7feaa321cb795
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.vehicle;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.IMinecartCollisionHandler;
import net.neoforged.neoforge.common.extensions.IAbstractMinecartExtension;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.entity.vehicle.AbstractMinecartInjection;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartInject extends Entity implements AbstractMinecartInjection, IAbstractMinecartExtension {
    public AbstractMinecartInject(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    private static IMinecartCollisionHandler COLLISIONS = null;

    public IMinecartCollisionHandler getCollisionHandler() {
        return COLLISIONS;
    }

    @CreateStatic
    private static void registerCollisionHandler(IMinecartCollisionHandler handler) {
        COLLISIONS = handler;
    }

    private boolean canUseRail = true;

    @Override
    public boolean canUseRail() {
        return canUseRail;
    }

}
