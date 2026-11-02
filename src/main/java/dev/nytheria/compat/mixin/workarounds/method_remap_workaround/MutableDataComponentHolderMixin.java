/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.workarounds.method_remap_workaround;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import net.neoforged.neoforge.common.MutableDataComponentHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.workarounds.MutableDataComponentHolderWorkaround;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;

@Implements(@Interface(iface = MutableDataComponentHolderWorkaround.class, prefix = "nytheriacompat$i$", remap = Interface.Remap.NONE))
@Mixin(MutableDataComponentHolder.class)
public interface MutableDataComponentHolderMixin {

    @Shadow
    @Nullable
    <T> T set(DataComponentType<? super T> dataComponentType, @Nullable T object);

    @Shadow
    @Nullable <T, U> T update(DataComponentType<T> componentType, T value, U updateContext, BiFunction<T, U, T> updater);

    @Shadow
    @Nullable <T> T update(DataComponentType<T> componentType, T value, UnaryOperator<T> updater);

    @Shadow
    @Nullable
    <T> T remove(DataComponentType<? extends T> dataComponentType);

    @Shadow
    void applyComponents(DataComponentPatch patch);

    @Shadow
    void applyComponents(DataComponentMap dataComponentMap);

    @Intrinsic
    default <T> T nytheriacompat$i$set(@NotNull DataComponentType<? super T> componentType, @Nullable T value) {
        return this.set(componentType, value);
    }

    @Intrinsic
    default <T> T nytheriacompat$i$remove(@NotNull DataComponentType<? super T> componentType) {
        //noinspection unchecked
        return this.remove((DataComponentType<? extends T>) componentType);
    }

    @Intrinsic
    default <T, U> T nytheriacompat$i$update(DataComponentType<T> componentType, T value, U updateContext, BiFunction<T, U, T> updater) {
        return this.update(componentType, value, updateContext, updater);
    }

    @Intrinsic
    default <T> T nytheriacompat$i$update(DataComponentType<T> componentType, T value, UnaryOperator<T> updater) {
        return this.update(componentType, value, updater);
    }

    @Intrinsic
    default void nytheriacompat$i$applyComponents(@NotNull DataComponentPatch patch) {
        this.applyComponents(patch);
    }

    @Intrinsic
    default void nytheriacompat$i$applyComponents(@NotNull DataComponentMap components) {
        this.applyComponents(components);
    }
}
