/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.flag;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.injections.world.flag.FeatureFlagInjection;
import dev.nytheria.compat.injections.world.flag.FeatureFlagRegistryInjection;

import java.util.Map;

@Mixin(FeatureFlagRegistry.class)
public abstract class FeatureFlagRegistryInject implements FeatureFlagRegistryInjection {
    @Shadow @Final private Map<ResourceLocation, FeatureFlag> names;

    @Override
    public Map<ResourceLocation, FeatureFlag> getAllFlags() {
        return this.names;
    }

    @Override
    public boolean hasAnyModdedFlags() {
        return this.names.values().stream().anyMatch(FeatureFlagInjection::isModded);
    }

    @Mixin(FeatureFlagRegistry.Builder.class)
    public static abstract class BuilderInject implements FeatureFlagRegistryInjection.BuilderInjection {
        @Shadow
        public abstract FeatureFlag create(ResourceLocation resourceLocation);

        @Unique private boolean nytheriacompat$isModded = false;

        @Override
        public FeatureFlag create(ResourceLocation id, boolean modded) {
            this.nytheriacompat$isModded = modded;
            FeatureFlag flag = this.create(id);
            this.nytheriacompat$isModded = false;

            return flag;
        }
    }
}
