// TRACKED HASH: eb3294cfe8207d695cc790cd2173da72510954e6
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import net.minecraft.world.item.DispensibleContainerItem;
import net.neoforged.neoforge.common.extensions.IDispensibleContainerItemExtension;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DispensibleContainerItem.class)
public interface DispensibleContainerItemInject extends IDispensibleContainerItemExtension {
}