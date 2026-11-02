/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.item;

import com.google.common.collect.Maps;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import dev.nytheria.compat.mixin.client.renderer.item.ItemPropertiesAccessor;

public interface ItemPropertiesInjection {
    static ItemPropertyFunction registerGeneric(ResourceLocation name, ItemPropertyFunction property) {
        ItemPropertiesAccessor.getGenericProperties().put(name, property);
        return property;
    }

    static void register(Item item, ResourceLocation name, ItemPropertyFunction property) {
        ItemPropertiesAccessor.getProperties().computeIfAbsent(item, $ -> Maps.newHashMap())
            .put(name, property);
    }
}
