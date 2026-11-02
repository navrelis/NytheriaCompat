/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.compat.fabric_api;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    @Shadow private Map<String, Pack> available;

    @Inject(method = "getPack", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$resolveFabricModResources(String id, CallbackInfoReturnable<Pack> cir) {
        if (id.equals("mod_resources")) {
            cir.setReturnValue(this.available.get("fabric"));
        }
    }
}
