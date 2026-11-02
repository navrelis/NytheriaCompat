/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.helpers;


import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.storage.loot.LootDataType;

// hey, you! have you ever wanted to mixin to records and add a field into them?
// don't! use this instead! some JVMs actually don't support that behavior and instead return invalid/default values!
public class RecordMixinRefMaps {
    private RecordMixinRefMaps() {}

    public static final Map<LootDataType<?>, ExtendedLootDataType<?>> EXTENDED_LOOT_DATA_TYPE = create();
    public static final Map<MobEffect.AttributeTemplate, ExtendedAttributeTemplate> EXTENDED_ATTRIBUTE_TEMPLATE = create();

    public record ExtendedLootDataType<T>(@Nullable T defaultValue, Codec<Optional<T>> conditionalCodec, BiConsumer<T, ResourceLocation> idSetter) {}
    public record ExtendedAttributeTemplate(@Nullable Int2DoubleFunction curve) {}

    private static <K, V> Map<K, V> create() {
        return new WeakHashMap<>();
    }
}
