/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.tags;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@FabricInjectedInterface(TagEntry.class)
public interface TagEntryInjection {
    default ResourceLocation getId() {
        throw NytheriaCompatHelper.createMixinException(TagEntry.class, "getId");
    }

    default boolean isRequired() {
        throw NytheriaCompatHelper.createMixinException(TagEntry.class, "isRequired");
    }

}
