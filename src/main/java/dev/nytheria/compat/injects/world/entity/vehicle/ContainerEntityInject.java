/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.vehicle;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.vehicle.ContainerEntity;

@Mixin(ContainerEntity.class)
public interface ContainerEntityInject {
}
