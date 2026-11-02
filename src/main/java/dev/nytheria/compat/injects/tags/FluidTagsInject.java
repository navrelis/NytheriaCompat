// TRACKED HASH: 820cf3db05b769c92b345f8be864731738c0d340
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.tags;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.tags.FluidTagsInjection;

@Mixin(FluidTags.class)
public class FluidTagsInject implements FluidTagsInjection {
    @CreateStatic
    private static TagKey<Fluid> create(ResourceLocation name) {
        return FluidTagsInjection.create(name);
    }
}