/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.biome;

import net.neoforged.fml.common.asm.enumextension.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.AbstractOverride;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.level.biome.BiomeSpecialEffectsInjection;

import net.minecraft.world.level.biome.BiomeSpecialEffects;

@Mixin(BiomeSpecialEffects.class)
public abstract class BiomeSpecialEffectsInject {
    @NamedEnum
    @NetworkedEnum(NetworkedEnum.NetworkCheck.CLIENTBOUND)
    @Mixin(BiomeSpecialEffects.GrassColorModifier.class)
    public abstract static class GrassColorModifierInject implements IExtensibleEnum, BiomeSpecialEffectsInjection.GrassColorModifierInjection {
        @Unique private ColorModifier delegate;

        @AbstractOverride
        public int modifyColor(double x, double z, int grassColor) {
            return this.delegate.modifyGrassColor(x, z, grassColor);
        }

        @ReservedConstructor
        GrassColorModifierInject(String fieldName, int ordinal, String name) {}

        @CreateInitializer
        GrassColorModifierInject(String fieldName, int ordinal, String name, ColorModifier delegate) {
            this(fieldName, ordinal, name);
            this.delegate = delegate;
        }

        public ColorModifier nytheriacompat$getDelegate() {
            return this.delegate;
        }

        @CreateStatic
        private static ExtensionInfo getExtensionInfo() {
            return ExtensionInfo.nonExtended(BiomeSpecialEffects.GrassColorModifier.class);
        }
    }
}
