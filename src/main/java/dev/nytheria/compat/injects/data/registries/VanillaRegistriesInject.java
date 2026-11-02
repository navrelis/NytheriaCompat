// TRACKED HASH: 7fb07c0db39d3d5c0d350e9748839bc08a66e387
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.data.registries;

import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.data.registries.VanillaRegistriesInjection;

import java.util.List;

@Mixin(VanillaRegistries.class)
public class VanillaRegistriesInject {
    @CreateStatic
    private static final List<? extends ResourceKey<? extends Registry<?>>> DATAPACK_REGISTRY_KEYS = VanillaRegistriesInjection.DATAPACK_REGISTRY_KEYS;
}