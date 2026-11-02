/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity.vehicle;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.neoforged.neoforge.common.IMinecartCollisionHandler;

public interface AbstractMinecartInjection extends io.github.fabricators_of_create.porting_lib.entity.injects.AbstractMinecartInjection {
    // only implemented here because i have a workaround mapping sitting around, i already removed it but i'm afraid
    // of the implications of if i were to refresh my remapped mods again, so let's just do reflection
    static void registerCollisionHandler(IMinecartCollisionHandler handler) {
        try {
            var method = AbstractMinecart.class.getDeclaredMethod("registerCollisionHandler", IMinecartCollisionHandler.class);
            method.invoke(null, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
