// TRACKED HASH: 5520e72d813534639a4a101cffa56e6e9fe409b0
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.level.levelgen.structure.templatesystem.StructureTemplateInjection;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateInject implements StructureTemplateInjection {
    @CreateStatic
    private static Vec3 forge$transformedVec3d(StructurePlaceSettings placementIn, Vec3 pos) {
        return StructureTemplateInjection.transformedVec3d(placementIn, pos);
    }

    @CreateStatic
    private static List<StructureTemplate.StructureEntityInfo> forge$processEntityInfos(@Nullable StructureTemplate template, LevelAccessor level, BlockPos blockPos, StructurePlaceSettings structurePlaceSettings, List<StructureTemplate.StructureEntityInfo> structureEntityInfoList) {
        return StructureTemplateInjection.processEntityInfos(template, level, blockPos, structurePlaceSettings, structureEntityInfoList);
    }

    // Nytheria Compat: worldgen worker threads and the server thread place structures at the same time, so the template
    // handed over by NeoForge's 6-arg processBlockInfos must be per-thread, not shared.
    private static final ThreadLocal<StructureTemplate> nytheriacompat$template = new ThreadLocal<>();

    @Shadow
    public static Vec3 transform(Vec3 target, Mirror mirror, Rotation rotation, BlockPos centerOffset) {
        return null;
    }

    @CreateStatic
    private static List<StructureTemplate.StructureBlockInfo> processBlockInfos(ServerLevelAccessor level, BlockPos pos, BlockPos pos2, StructurePlaceSettings structurePlaceSettings, List<StructureTemplate.StructureBlockInfo> list, @Nullable StructureTemplate template) {
        // Nytheria Compat: restore the previous value afterwards so nested placements on the same thread keep working.
        var previous = nytheriacompat$template.get();
        nytheriacompat$template.set(template);
        try {
            return StructureTemplate.processBlockInfos(level, pos, pos2, structurePlaceSettings, list);
        } finally {
            if (previous == null)
                nytheriacompat$template.remove();
            else
                nytheriacompat$template.set(previous);
        }
    }

    @Mixin(StructureTemplate.Palette.class)
    public abstract static class PaletteInject {
        @Shadow @Final @Mutable private Map<Block, List<StructureTemplate.StructureBlockInfo>> cache;

        // Neo: Fixes MC-271899 - templates are shared by all worldgen threads and the server thread, and the vanilla
        // HashMap cache throws ConcurrentModificationException when two threads fill it at the same time.
        @Inject(method = "<init>", at = @At("RETURN"))
        private void nytheriacompat$useConcurrentCache(List<StructureTemplate.StructureBlockInfo> blocks, CallbackInfo ci) {
            this.cache = Maps.newConcurrentMap();
        }
    }
}