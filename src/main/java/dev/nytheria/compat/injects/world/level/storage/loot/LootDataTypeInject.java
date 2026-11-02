/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.storage.loot;

import java.util.Optional;
import java.util.function.BiConsumer;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.RecordMixinRefMaps;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.world.level.storage.loot.LootDataTypeInjection;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataType;

@Mixin(LootDataType.class)
public abstract class LootDataTypeInject<T> implements LootDataTypeInjection<T> {
    @Shadow
    public abstract Codec<T> codec();

    public LootDataTypeInject(ResourceKey<Registry<T>> registryKey, Codec<T> codec, LootDataType.Validator<T> validator) {}

    @CreateInitializer
    public LootDataTypeInject(ResourceKey<Registry<T>> registryKey, Codec<T> codec, LootDataType.Validator<T> validator, @Nullable T defaultValue, BiConsumer<T, ResourceLocation> idSetter) {
        this(registryKey, codec, validator, defaultValue, ConditionalOps.createConditionalCodec(codec), idSetter);
    }

    @CreateInitializer
    public LootDataTypeInject(ResourceKey<Registry<T>> registryKey, Codec<T> codec, LootDataType.Validator<T> validator, @Nullable T defaultValue, Codec<Optional<T>> conditionalCodec, BiConsumer<T, ResourceLocation> idSetter) {
        this(registryKey, codec, validator);
        RecordMixinRefMaps.EXTENDED_LOOT_DATA_TYPE.put((LootDataType<?>) (Object) this, new RecordMixinRefMaps.ExtendedLootDataType<>(defaultValue, conditionalCodec, idSetter));
    }

    @Unique
    private RecordMixinRefMaps.ExtendedLootDataType<T> nytheriacompat$getExtended() {
        var codec = this.codec();
        return (RecordMixinRefMaps.ExtendedLootDataType<T>) RecordMixinRefMaps.EXTENDED_LOOT_DATA_TYPE.computeIfAbsent((LootDataType<?>) (Object) this, $ -> new RecordMixinRefMaps.ExtendedLootDataType<>(null, ConditionalOps.createConditionalCodec(codec), (it, id) -> {}));
    }

    @Override
    public @Nullable T defaultValue() {
        return this.nytheriacompat$getExtended().defaultValue();
    }

    @Override
    public Codec<Optional<T>> conditionalCodec() {
        return this.nytheriacompat$getExtended().conditionalCodec();
    }

    @Override
    public BiConsumer<T, ResourceLocation> idSetter() {
        return this.nytheriacompat$getExtended().idSetter();
    }

    // Nytheria Compat: it doesn't look like anything changed here???
}
