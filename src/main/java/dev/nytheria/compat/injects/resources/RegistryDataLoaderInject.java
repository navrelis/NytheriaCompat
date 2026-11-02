/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.resources;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.resources.RegistryDataLoaderInjection;
import dev.nytheria.compat.util.NeoForgeSources;

import java.util.Map;
import java.util.function.Consumer;

@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderInject implements RegistryDataLoaderInjection {


    /**
     * Nytheria Compat: scope of one registry's load from the data packs, for the decisions about non-NeoForge packs' data in
     * {@code loadElementFromResource}. The counters live in a thread local that is set for exactly this call and
     * restored in {@code finally}: registries load one after another on one thread, and nothing survives the call.
     */
    @WrapMethod(method = "loadContentsFromManager")
    private static <E> void nytheriacompat$scopeFabricModData(ResourceManager resourceManager, RegistryOps.RegistryInfoLookup registryInfoLookup, WritableRegistry<E> registry, Decoder<E> decoder, Map<ResourceKey<?>, Exception> errors, Operation<Void> original) {
        NeoForgeSources.RegistryLoad scope = NeoForgeSources.RegistryLoad.begin(registry.key().location());
        try {
            original.call(resourceManager, registryInfoLookup, registry, decoder, errors);
        } finally {
            scope.end();
        }
    }

    /**
     * Nytheria Compat: biome and structure modifiers are only loaded from NeoForge mods (and Nytheria Compat's own packs); plain Fabric
     * ignores them everywhere else, and Fabric mods that ship them make the same change through Fabric API, often behind
     * a config toggle.
     */
    @Inject(method = "loadElementFromResource", at = @At("HEAD"), cancellable = true)
    private static <E> void nytheriacompat$skipNonNeoForgeModifiers(WritableRegistry<E> registry, Decoder<E> decoder, RegistryOps<JsonElement> ops, ResourceKey<E> key, Resource resource, RegistrationInfo registrationInfo, CallbackInfo ci) {
        if (NeoForgeSources.isModifierRegistry(registry.key().location()) && NeoForgeSources.ignoresNeoForgeData(resource.sourcePackId())) {
            NeoForgeSources.RegistryLoad.ignoredModifier(resource.sourcePackId(), key.location());
            ci.cancel();
        }
    }

    @Mixin(RegistryDataLoader.RegistryData.class)
    public abstract static class RegistryDataInject<T> implements RegistryDataLoaderInjection.RegistryDataInjection<T> {
        @Unique private Consumer<RegistryBuilder<T>> registryBuilderConsumer = registryBuilder -> {};

        public RegistryDataInject(ResourceKey<? extends Registry<T>> key, Codec<T> elementCodec, boolean requiredNonEmpty) {
        }

        @CreateInitializer
        public RegistryDataInject(ResourceKey<? extends Registry<T>> key, Codec<T> elementCodec, boolean requiredNonEmpty, Consumer<RegistryBuilder<T>> registryBuilderConsumer) {
            this(key, elementCodec, requiredNonEmpty);
            this.registryBuilderConsumer = registryBuilderConsumer;
        }

        @Override
        public void nytheriacompat$setRegistryBuilderConsumer(Consumer<RegistryBuilder<T>> builderConsumer) {
            this.registryBuilderConsumer = builderConsumer;
        }

        @WrapOperation(method = "create", at = @At(value = "NEW", target = "(Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Lifecycle;)Lnet/minecraft/core/MappedRegistry;"))
        private MappedRegistry<T> nytheriacompat$createRegistry(ResourceKey<? extends Registry<T>> resourceKey, Lifecycle lifecycle, Operation<MappedRegistry<T>> original) {
            var registryBuilder = new RegistryBuilder<>(resourceKey);
            this.registryBuilderConsumer.accept(registryBuilder);

            return (MappedRegistry<T>) registryBuilder.disableRegistrationCheck().create();
        }
    }
}
