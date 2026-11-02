/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.resources;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.function.Consumer;

public interface RegistryDataLoaderInjection {
    interface RegistryDataInjection<T> {
        default Consumer<RegistryBuilder<T>> registryBuilderConsumer() {
            throw NytheriaCompatHelper.createMixinException(RegistryDataInjection.class, "registryBuilderConsumer");
        }

        default void nytheriacompat$setRegistryBuilderConsumer(Consumer<RegistryBuilder<T>> builderConsumer) {
            throw NytheriaCompatHelper.createMixinException(RegistryDataInjection.class, "nytheriacompat$setRegistryBuilderConsumer");
        }

        static <T> RegistryDataLoader.RegistryData<T> create(ResourceKey<? extends Registry<T>> key, Codec<T> elementCodec, boolean requiredNonEmpty, Consumer<RegistryBuilder<T>> builderConsumer) {
            var registryData = new RegistryDataLoader.RegistryData<>(key, elementCodec, requiredNonEmpty);
            registryData.nytheriacompat$setRegistryBuilderConsumer(builderConsumer);
            return registryData;
        }
    }
}
