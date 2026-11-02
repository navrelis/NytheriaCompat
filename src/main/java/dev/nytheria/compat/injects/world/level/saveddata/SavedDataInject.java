/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.saveddata;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.world.level.saveddata.SavedDataInjection;

import java.util.function.BiFunction;
import java.util.function.Supplier;

@Mixin(SavedData.class)
public abstract class SavedDataInject implements SavedDataInjection {
    // Nytheria Compat TODO: do we need to use the IO worker??

    @ModifyArg(method = "save(Ljava/io/File;Lnet/minecraft/core/HolderLookup$Provider;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/nio/file/Path;)V"))
    private CompoundTag nytheriacompat$copyNbtTag(CompoundTag compoundTag) {
        return compoundTag.copy();
    }

    @Mixin(SavedData.Factory.class)
    public abstract static class FactoryInject<T extends SavedData> implements SavedDataInjection.FactoryInjection {
        public FactoryInject(Supplier<T> constructor, BiFunction<CompoundTag, HolderLookup.Provider, T> deserializer, DataFixTypes type) {
        }

        @CreateInitializer
        public FactoryInject(Supplier<T> constructor, BiFunction<CompoundTag, HolderLookup.Provider, T> deserializer) {
            this(constructor, deserializer, null);
        }
    }
}
