/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.client.renderer.item;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemProperties.class)
public interface ItemPropertiesAccessor {
    @Accessor("GENERIC_PROPERTIES")
    static Map<ResourceLocation, ItemPropertyFunction> getGenericProperties() {
        throw new UnsupportedOperationException();
    }

    @Accessor("PROPERTIES")
    static Map<Item, Map<ResourceLocation, ItemPropertyFunction>> getProperties() {
        throw new UnsupportedOperationException();
    }
}
