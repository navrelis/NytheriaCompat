/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.multiplayer;

import net.minecraft.client.multiplayer.TagCollector;
import net.minecraft.core.RegistryAccess;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.world.item.crafting.IngredientInjection;

@Mixin(TagCollector.class)
public abstract class TagCollectorInject {
    @Inject(method = "updateTags", at = @At("TAIL"))
    private void nytheriacompat$callTagsUpdatedEvent(RegistryAccess registryAccess, boolean isMemoryConnection, CallbackInfo ci) {
        // Nytheria Compat: ingredients that were asked for their items before any tags were bound resolve them again
        IngredientInjection.EMPTY_BEFORE_TAGS.tagsBound();
        if (EventGate.has(TagsUpdatedEvent.class)) NeoForge.EVENT_BUS.post(new TagsUpdatedEvent(registryAccess, true, isMemoryConnection));
    }
}
