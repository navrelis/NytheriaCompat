/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface FireBlockInjection {
    default boolean canCatchFire(BlockGetter level, BlockPos pos, Direction face) {
        throw NytheriaCompatHelper.createMixinException(FireBlockInjection.class, "canCatchFire");
    }
}
