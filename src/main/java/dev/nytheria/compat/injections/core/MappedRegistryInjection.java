/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.core;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@FabricInjectedInterface(MappedRegistry.class)
public interface MappedRegistryInjection<T> {
    default void unfreeze() {
        throw new IllegalStateException();
    }

    default Holder.Reference<T> register(int id, ResourceKey<T> key, T value, RegistrationInfo info) {
        throw new IllegalStateException();
    }

    default void registerIdMapping(ResourceKey<T> key, int id) {
        throw NytheriaCompatHelper.createMixinException(MappedRegistryInjection.class, "registerIdMapping");
    }

    default void clear(boolean full) {
        throw NytheriaCompatHelper.createMixinException(MappedRegistryInjection.class, "clear");
    }

    // Nytheria Compat: mainly used to call the super, honestly.
    default void nytheriacompat$clear(boolean full) {}
}
