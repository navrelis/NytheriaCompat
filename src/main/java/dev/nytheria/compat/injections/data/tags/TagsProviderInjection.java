/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.data.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@FabricInjectedInterface(TagsProvider.class)
public interface TagsProviderInjection {
    default void nytheriacompat$setModId(String modId) {
        throw new IllegalStateException();
    }

    default void nytheriacompat$setExistingFileHelper(ExistingFileHelper fileHelper) {
        throw new IllegalStateException();
    }

    default void nytheriacompat$addConstructorArgs(String modId, ExistingFileHelper fileHelper) {
        this.nytheriacompat$setModId(modId);
        this.nytheriacompat$setExistingFileHelper(fileHelper);
    }

    interface TagAppenderInjection {
        default TagBuilder getInternalBuilder() {
            throw NytheriaCompatHelper.createMixinException(TagAppenderInjection.class, "getInternalBuilder");
        }

        default String getModID() {
            throw NytheriaCompatHelper.createMixinException(TagAppenderInjection.class, "getModID");
        }
    }
}
