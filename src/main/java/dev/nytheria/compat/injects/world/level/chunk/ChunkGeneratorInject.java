/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.chunk;

import com.google.common.base.Supplier;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.common.util.Lazy;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.injections.world.level.chunk.ChunkGeneratorInjection;

import java.util.List;

@Mixin(value = ChunkGenerator.class, priority = 10000) // We want our mixin to come in as late as possible to capture everyone.
public abstract class ChunkGeneratorInject implements ChunkGeneratorInjection {
    @Shadow @Final @Mutable private java.util.function.Supplier<List<FeatureSorter.StepFeatureData>> featuresPerStep;
    @Unique private Lazy<List<FeatureSorter.StepFeatureData>> nytheriacompat$lazyFeaturesPerStep;

    @WrapOperation(method = "<init>(Lnet/minecraft/world/level/biome/BiomeSource;Ljava/util/function/Function;)V", at = @At(value = "INVOKE", target = "Lcom/google/common/base/Suppliers;memoize(Lcom/google/common/base/Supplier;)Lcom/google/common/base/Supplier;"))
    private <T> Supplier<T> nytheriacompat$storeLazyFeatures(Supplier<T> delegate, Operation<Supplier<T>> original) {
        Supplier<T> memoized = original.call(delegate);
        this.nytheriacompat$lazyFeaturesPerStep = Lazy.of(() -> ((Supplier<List<FeatureSorter.StepFeatureData>>) delegate).get());
        return memoized;
    }

    @Override
    public void refreshFeaturesPerStep() {
        this.nytheriacompat$lazyFeaturesPerStep.invalidate();
        this.featuresPerStep = this.nytheriacompat$lazyFeaturesPerStep;
    }
}
