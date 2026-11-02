/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.tags;

import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.stream.Stream;

@FabricInjectedInterface(TagBuilder.class)
public interface TagBuilderInjection {
    default TagBuilder remove(final TagEntry entry) {
        throw new IllegalStateException();
    }

    default Stream<TagEntry> getRemoveEntries() {
        throw new IllegalStateException();
    }

    default TagBuilder replace(boolean value) {
        throw new IllegalStateException();
    }

    default TagBuilder replace() {
        return replace(true);
    }

    default boolean isReplace() {
        throw NytheriaCompatHelper.createMixinException(TagBuilderInjection.class, "isReplace");
    }
}
