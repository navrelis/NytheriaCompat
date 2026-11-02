/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package xyz.bluspring.kilt.injections.client.renderer.entity;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.ListModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * Compatibility alias under the upstream package name, kept on purpose. Twilight Forest (Fabric) ships mixins for the
 * upstream project that implement this exact interface name ({@code twilightforest.mixin.compat.KiltBoatRendererMixin}
 * and {@code KiltBoatRendererCompatibilityMixin}, enabled when the mod ID {@code kilt} is loaded, which Nytheria Compat
 * provides). The method is the same as {@link dev.nytheria.compat.injections.client.renderer.entity.BoatRendererInjection}'s,
 * so their implementation also serves Nytheria Compat's interface.
 */
public interface BoatRendererInjection extends dev.nytheria.compat.injections.client.renderer.entity.BoatRendererInjection {
    @Override
    Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat);
}
