/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import com.google.common.base.Suppliers;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.fml.common.asm.enumextension.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.item.ItemDisplayContextInjection;

import java.util.function.Supplier;

@IndexedEnum
@NamedEnum(1)
@NetworkedEnum(NetworkedEnum.NetworkCheck.CLIENTBOUND)
@Mixin(ItemDisplayContext.class)
public abstract class ItemDisplayContextInject implements IExtensibleEnum, ItemDisplayContextInjection {
    @Unique private boolean isModded = false;
    @Unique private Supplier<ItemDisplayContext> fallback = () -> null;

    @ReservedConstructor
    private ItemDisplayContextInject(String fieldName, int ordinal, int id, String name) {
    }

    @CreateInitializer
    private ItemDisplayContextInject(String fieldName, int ordinal, int id, String name, @Nullable String fallbackName) {
        this(fieldName, ordinal, id, name);
        this.isModded = true;
        this.fallback = fallbackName == null ? () -> null : Suppliers.memoize(() -> ItemDisplayContext.valueOf(fallbackName));
    }

    @Override
    public boolean isModded() {
        return this.isModded;
    }

    @Override
    public @Nullable ItemDisplayContext fallback() {
        return this.fallback.get();
    }

    @CreateStatic
    private static ExtensionInfo getExtensionInfo() {
        return ExtensionInfo.nonExtended(ItemDisplayContext.class);
    }
}
