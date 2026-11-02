/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.levelgen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import dev.nytheria.compat.mixin.DebugLevelSourceAccessor;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface DebugLevelSourceInjection {
    static void initValidStates() {
        // f_64114_ = ALL_BLOCKS
        // f_64115_ = GRID_WIDTH
        // f_64116_ = GRID_HEIGHT

        DebugLevelSourceAccessor.setAllBlocks(
                StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false).flatMap((block) ->
                    block.getStateDefinition().getPossibleStates().stream()
                ).collect(Collectors.toList())
        );
        DebugLevelSourceAccessor.setGridWidth(Mth.ceil(Mth.sqrt((float) DebugLevelSourceAccessor.getAllBlocks().size())));
        DebugLevelSourceAccessor.setGridHeight(
                (int) ((float) DebugLevelSourceAccessor.getAllBlocks().size() / (float) DebugLevelSourceAccessor.getGridWidth())
        );
    }
}
