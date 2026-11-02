// TRACKED HASH: 0c86e781bc3655f83ffbb2cfb5802edb5d644707
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.tags;

import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.neoforged.neoforge.common.extensions.ITagBuilderExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.injections.tags.TagBuilderInjection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Mixin(TagBuilder.class)
public class TagBuilderInject implements TagBuilderInjection, ITagBuilderExtension {
    private final List<TagEntry> removeEntries = new ArrayList<>();

    @Override
    public Stream<TagEntry> getRemoveEntries() {
        return removeEntries.stream();
    }

    @Override
    public TagBuilder remove(TagEntry entry) {
        removeEntries.add(entry);
        return (TagBuilder) (Object) this;
    }

    @Unique private boolean replace = false;

    @Override
    public TagBuilder replace(boolean value) {
        this.replace = value;
        return (TagBuilder) (Object) this;
    }

    public TagBuilder replace() {
        return this.replace(true);
    }

    @Override
    public boolean isReplace() {
        return replace;
    }
}