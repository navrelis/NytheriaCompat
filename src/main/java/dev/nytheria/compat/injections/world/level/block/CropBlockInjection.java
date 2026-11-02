/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.block;

import net.minecraft.world.level.block.state.BlockState;

public interface CropBlockInjection {
    ThreadLocal<BlockState> nytheriacompat$currentState = new ThreadLocal<>();
}
