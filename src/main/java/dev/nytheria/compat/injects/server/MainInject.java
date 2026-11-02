/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server;

import net.minecraft.server.Main;
import net.neoforged.neoforge.server.loading.ServerModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.util.NytheriaCompatTimings;

@Mixin(Main.class)
public abstract class MainInject {
    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;startTimerHackThread()V", shift = At.Shift.AFTER))
    private static void nytheriacompat$initForgeLoader(String[] strings, CallbackInfo ci) {
        long start = System.nanoTime();
        ServerModLoader.load();
        NytheriaCompatTimings.log("ServerModLoader.load", System.nanoTime() - start);
    }

    // TODO: oh jesus christ good luck.
}
