/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin;

import com.google.common.collect.Interner;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Item.Properties.class)
public interface ItemPropertiesAccessor {
    @Accessor("COMPONENT_INTERNER")
    static Interner<DataComponentMap> getComponentInterner() {
        throw new UnsupportedOperationException();
    }

    @Invoker
    DataComponentMap callBuildAndValidateComponents();
}
