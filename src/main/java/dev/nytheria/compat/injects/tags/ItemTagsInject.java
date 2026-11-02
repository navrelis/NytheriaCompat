// TRACKED HASH: 19c8a1dd76d76397514e761f191accdc8254200b
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.tags;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.tags.ItemTagsInjection;

@Mixin(ItemTags.class)
public class ItemTagsInject implements ItemTagsInjection {
    @CreateStatic
    private static TagKey<Item> create(ResourceLocation name) {
        return ItemTagsInjection.create(name);
    }
}