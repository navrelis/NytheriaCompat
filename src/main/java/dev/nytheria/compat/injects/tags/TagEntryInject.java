/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.tags;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.tags.TagEntryInjection;

@Mixin(TagEntry.class)
public abstract class TagEntryInject implements TagEntryInjection {
    @Shadow @Final private ResourceLocation id;
    @Shadow @Final private boolean required;

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

}
