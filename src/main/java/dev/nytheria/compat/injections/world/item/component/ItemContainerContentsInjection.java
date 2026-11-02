/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item.component;

import net.minecraft.world.item.ItemStack;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface ItemContainerContentsInjection {
    default int getSlots() {
        throw NytheriaCompatHelper.createMixinException(ItemContainerContentsInjection.class, "getSlots");
    }

    default ItemStack getStackInSlot(int slot) {
        throw NytheriaCompatHelper.createMixinException(ItemContainerContentsInjection.class, "getStackInSlot");
    }
}
