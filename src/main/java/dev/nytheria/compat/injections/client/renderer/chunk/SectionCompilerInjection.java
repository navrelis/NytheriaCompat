/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.chunk;

import java.util.List;

import com.mojang.blaze3d.vertex.VertexSorting;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;

public interface SectionCompilerInjection {
    default void nytheriacompat$setAdditionalRenderers(List<AddSectionGeometryEvent.AdditionalSectionRenderer> renderers) {
        throw NytheriaCompatHelper.createMixinException(SectionCompilerInjection.class, "nytheriacompat$setAdditionalRenderers");
    }

    default SectionCompiler.Results compile(SectionPos pos, RenderChunkRegion region, VertexSorting sorting, SectionBufferBuilderPack bufferBuilderPack, List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers) {
        throw NytheriaCompatHelper.createMixinException(SectionCompilerInjection.class, "compile");
    }
}
