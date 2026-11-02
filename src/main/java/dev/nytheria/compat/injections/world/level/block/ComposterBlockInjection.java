/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.block;

import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import net.minecraft.world.item.ItemStack;

public interface ComposterBlockInjection {
    static float getValue(ItemStack item) {
        var value = item.getItemHolder().getData(NeoForgeDataMaps.COMPOSTABLES);
        if (value != null)
            return value.chance();

        return -1f;
    }
}
