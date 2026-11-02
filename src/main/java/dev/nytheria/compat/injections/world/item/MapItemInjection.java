/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface MapItemInjection {
    default MapItemSavedData getCustomMapData(ItemStack stack, Level level) {
        throw NytheriaCompatHelper.createMixinException(MapItemInjection.class, "getCustomMapData");
    }
}
