/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.compat.fabric_api;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback;
import net.fabricmc.fabric.impl.registry.sync.trackers.Int2ObjectMapTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Int2ObjectMapTracker.class, remap = false)
public abstract class Int2ObjectMapTrackerMixin<V, OV> {
    @Shadow @Final private Int2ObjectMap<OV> mappers;

    @Inject(method = "onRemap", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$avoidModernFixResourcesCrash(RegistryIdRemapCallback.RemapState<V> state, CallbackInfo ci) {
        if (this.mappers.isEmpty())
            ci.cancel();
    }
}
