// TRACKED HASH: 5bd8da3c51f4d0280c72682973eb6e5b67353690
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.world.inventory.SlotInjection;

@Mixin(Slot.class)
public abstract class SlotInject implements SlotInjection {
    @Shadow @Final public Container container;

    @Override
    public boolean isSameInventory(Slot other) {
        return this.container == other.container;
    }

    // Nytheria Compat: background & slot index handled by Porting Lib
}