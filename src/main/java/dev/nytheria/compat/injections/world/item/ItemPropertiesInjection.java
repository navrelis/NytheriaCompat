/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.Item;
import dev.nytheria.compat.mixin.ItemPropertiesAccessor;

public interface ItemPropertiesInjection {
    default boolean getCanRepair() {
        throw new IllegalStateException();
    }

    default Item.Properties setNoRepair() {
        throw new IllegalStateException();
    }

    static DataComponentMap validateComponents(DataComponentMap map) {
        // Nytheria Compat: We want to be able to have other mods' mixins, so let's recreate what this patch basically does.
        var properties = new Item.Properties();
        for (TypedDataComponent<?> component : map) {
            properties.component((DataComponentType) component.type(), component.value());
        }

        return ((ItemPropertiesAccessor) properties).callBuildAndValidateComponents();
    }
}
