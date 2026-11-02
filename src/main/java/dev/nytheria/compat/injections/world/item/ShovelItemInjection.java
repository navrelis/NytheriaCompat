/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.state.BlockState;

public interface ShovelItemInjection {
    static BlockState getShovelPathingState(BlockState originalState) {
        return ShovelItem.FLATTENABLES.get(originalState.getBlock());
    }
}
