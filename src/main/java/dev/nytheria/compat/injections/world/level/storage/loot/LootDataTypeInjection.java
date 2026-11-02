/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.storage.loot;

import java.util.Optional;
import java.util.function.BiConsumer;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.resources.ResourceLocation;

public interface LootDataTypeInjection<T> {
    default @Nullable T defaultValue() {
        throw NytheriaCompatHelper.createMixinException(LootDataTypeInjection.class, "defaultValue");
    }

    default Codec<Optional<T>> conditionalCodec() {
        throw NytheriaCompatHelper.createMixinException(LootDataTypeInjection.class, "conditionalCodec");
    }

    default BiConsumer<T, ResourceLocation> idSetter() {
        throw NytheriaCompatHelper.createMixinException(LootDataTypeInjection.class, "idSetter");
    }
}
