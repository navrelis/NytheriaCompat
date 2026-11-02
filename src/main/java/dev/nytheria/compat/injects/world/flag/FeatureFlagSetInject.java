/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.flag;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.world.flag.FeatureFlagSetInjection;

import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlagUniverse;

@Mixin(FeatureFlagSet.class)
public abstract class FeatureFlagSetInject implements FeatureFlagSetInjection {
    @Unique private static final long[] EMPTY_EXT_MASK = new long[0];

    @Unique private long[] extendedMask = EMPTY_EXT_MASK;

    private FeatureFlagSetInject(@Nullable FeatureFlagUniverse universe, long mask) {
    }

    @CreateInitializer
    private FeatureFlagSetInject(@Nullable FeatureFlagUniverse universe, long mask, long[] extendedMask) {
        this(universe, mask);
        this.extendedMask = extendedMask;
    }

    @Override
    public long[] nytheriacompat$extendedMask() {
        return this.extendedMask;
    }

    @Override
    public void nytheriacompat$setExtendedMask(long[] extendedMask) {
        this.extendedMask = extendedMask;
    }
}
