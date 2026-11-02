// TRACKED HASH: 0ce8f1794babd31dbd3e815bf72e006d5b2176f6
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.item;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.client.renderer.item.ItemPropertiesInjection;

@Mixin(ItemProperties.class)
public abstract class ItemPropertiesInject {
    @CreateStatic
    private static ItemPropertyFunction registerGeneric(ResourceLocation name, ItemPropertyFunction property) {
        return ItemPropertiesInjection.registerGeneric(name, property);
    }

    @CreateStatic
    private static void register(Item item, ResourceLocation name, ItemPropertyFunction property) {
        ItemPropertiesInjection.register(item, name, property);
    }
}