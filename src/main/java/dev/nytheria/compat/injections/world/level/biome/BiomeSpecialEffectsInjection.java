/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.biome;

import net.minecraft.world.level.biome.BiomeSpecialEffects;

import java.util.HashMap;
import java.util.Map;

// fuck it i'm making it necessary
public interface BiomeSpecialEffectsInjection {
    interface GrassColorModifierInjection {
        Map<String, BiomeSpecialEffects.GrassColorModifier> BY_NAME = new HashMap<>();

        @FunctionalInterface
        public interface ColorModifier {
            int modifyGrassColor(double x, double z, int color);
        }

    }
}
