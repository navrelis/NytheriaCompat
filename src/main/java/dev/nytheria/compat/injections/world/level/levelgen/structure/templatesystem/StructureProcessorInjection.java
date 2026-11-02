/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.levelgen.structure.templatesystem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import javax.annotation.Nullable;

public interface StructureProcessorInjection {
    @Nullable
    default StructureTemplate.StructureBlockInfo process(LevelReader level, BlockPos blockPos, BlockPos pos, StructureTemplate.StructureBlockInfo blockInfo, StructureTemplate.StructureBlockInfo relativeBlockInfo, StructurePlaceSettings settings, @org.jetbrains.annotations.Nullable StructureTemplate template) {
        throw NytheriaCompatHelper.createMixinException(StructureProcessorInjection.class, "process");
    }

    default StructureTemplate.StructureEntityInfo processEntity(LevelReader world, BlockPos seedPos, StructureTemplate.StructureEntityInfo rawEntityInfo, StructureTemplate.StructureEntityInfo entityInfo, StructurePlaceSettings placementSettings, StructureTemplate template) {
        throw NytheriaCompatHelper.createMixinException(StructureProcessorInjection.class, "processEntity");
    }
}
