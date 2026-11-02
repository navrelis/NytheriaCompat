/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.block.entity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.Set;

public interface BlockEntityTypeInjection<T extends BlockEntity> {
    default Set<Block> getValidBlocks() {
        throw NytheriaCompatHelper.createMixinException(BlockEntityTypeInjection.class, "getValidBlocks");
    }
}
