/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.chunk;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.client.renderer.chunk.SectionRenderDispatcherInjection;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherInject {
    @Mixin(SectionRenderDispatcher.RenderSection.class)
    public abstract static class RenderSectionInject {
        @Shadow @Final private BlockPos.MutableBlockPos origin;
        @Shadow @Final private SectionRenderDispatcher field_20833;

        @WrapOperation(method = "createCompileTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderRegionCache;createRegion(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/SectionPos;)Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;"))
        private RenderChunkRegion nytheriacompat$setRegionEmpty(RenderRegionCache instance, Level level, SectionPos sectionPos, Operation<RenderChunkRegion> original, @Share("additionalRenderers") LocalRef<List<AddSectionGeometryEvent.AdditionalSectionRenderer>> additionalRenderers) {
            try {
                additionalRenderers.set(ClientHooks.gatherAdditionalRenderers(this.origin, field_20833.level));
                instance.nytheriacompat$setNullForEmpty(additionalRenderers.get().isEmpty());
                //noinspection MixinExtrasOperationParameters
                return original.call(instance, level, sectionPos);
            } finally {
                instance.nytheriacompat$setNullForEmpty(true);
            }
        }

        @Mixin(SectionRenderDispatcher.RenderSection.RebuildTask.class)
        public abstract static class RebuildTaskInject implements SectionRenderDispatcherInjection.RenderSectionInjection.RebuildTaskInjection {
            @Shadow
            @Final
            private SectionRenderDispatcher.RenderSection field_20839;
            @Unique private List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers = List.of();

            public RebuildTaskInject(double distAtCreation, @Nullable RenderChunkRegion region, boolean isHighPriority) {
            }

            @CreateInitializer
            public RebuildTaskInject(double distAtCreation, @Nullable RenderChunkRegion region, boolean isHighPriority, List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers) {
                this(distAtCreation, region, isHighPriority);
                this.additionalRenderers = additionalRenderers;
            }

            @Override
            public void nytheriacompat$setAdditionalRenderers(List<AddSectionGeometryEvent.AdditionalSectionRenderer> renderers) {
                this.additionalRenderers = renderers;
            }
        }
    }
}
