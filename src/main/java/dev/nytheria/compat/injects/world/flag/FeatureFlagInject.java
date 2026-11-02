/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.flag;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.world.flag.FeatureFlagInjection;

import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagUniverse;

@Mixin(FeatureFlag.class)
public abstract class FeatureFlagInject implements FeatureFlagInjection {
    @Shadow @Final private FeatureFlagUniverse universe;
    @Shadow @Final private long mask;

    @Unique int extMaskIndex = -1;
    @Unique boolean modded = false;

    FeatureFlagInject(FeatureFlagUniverse universe, int mask) {}

    @CreateInitializer
    FeatureFlagInject(FeatureFlagUniverse universe, int mask, int offset, boolean modded) {
        this(universe, mask);
        this.extMaskIndex = offset - 1;
        this.modded = modded;
    }

    @Override
    public boolean isModded() {
        return modded;
    }

    @Override
    public void nytheriacompat$setModded(boolean modded) {
        this.modded = modded;
    }

    @Override
    public int nytheriacompat$extMaskIndex() {
        return this.extMaskIndex;
    }

    @Override
    public void nytheriacompat$setExtMaskIndex(int index) {
        this.extMaskIndex = index;
    }

    @Override
    public FeatureFlagUniverse nytheriacompat$universe() {
        return this.universe;
    }

    @Override
    public long nytheriacompat$mask() {
        return this.mask;
    }
}
