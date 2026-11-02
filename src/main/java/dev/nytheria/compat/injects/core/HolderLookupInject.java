/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.extensions.IHolderLookupProviderExtension;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.core.HolderLookupInjection;

@Mixin(HolderLookup.class)
public interface HolderLookupInject {

    @Mixin(HolderLookup.Provider.class)
    public interface ProviderInject extends IHolderLookupProviderExtension {
    }

    @Mixin(HolderLookup.RegistryLookup.class)
    public interface RegistryLookupInject<T> extends HolderLookupInjection.RegistryLookupInjection<T> {
        @Intrinsic
        @Override
        default <A> @Nullable A getData(DataMapType<T, A> attachment, ResourceKey<T> key) {
            return null;
        }

        @Mixin(HolderLookup.RegistryLookup.Delegate.class)
        public interface DelegateInject<T> extends HolderLookupInjection.RegistryLookupInjection<T> {
            @Shadow HolderLookup.RegistryLookup<T> parent();

            @Override
            default <A> @Nullable A getData(DataMapType<T, A> attachment, ResourceKey<T> key) {
                return ((HolderLookupInjection.RegistryLookupInjection<T>) this.parent()).getData(attachment, key);
            }
        }
    }
}
