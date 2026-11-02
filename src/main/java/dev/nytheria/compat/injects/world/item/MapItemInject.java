/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.world.item.MapItemInjection;

@Mixin(MapItem.class)
public abstract class MapItemInject implements MapItemInjection {
    @Shadow
    @Nullable
    public static MapItemSavedData getSavedData(@Nullable MapId mapId, Level level) {
        throw new IllegalStateException();
    }

    @Override
    public MapItemSavedData getCustomMapData(ItemStack stack, Level level) {
        var id = stack.get(DataComponents.MAP_ID);
        return getSavedData(id, level);
    }
}
