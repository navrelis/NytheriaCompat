// TRACKED HASH: 1d41e3616afa3056987032c75858fb30258b2c0c
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.core;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.neoforged.neoforge.common.extensions.IHolderSetExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(HolderSet.class)
public interface HolderSetInject<T> extends IHolderSetExtension<T> {
    @Mixin(HolderSet.Named.class)
    class NamedInject<T> implements IHolderSetExtension<T> {
        private final List<Runnable> invalidationCallbacks = new ArrayList<>();

        @Override
        public void addInvalidationListener(@NotNull Runnable runnable) {
            invalidationCallbacks.add(runnable);
        }

        @Inject(at = @At("TAIL"), method = "bind")
        public void nytheriacompat$invalidateCallbacks(List<Holder<T>> list, CallbackInfo ci) {
            for (Runnable invalidationCallback : this.invalidationCallbacks) {
                invalidationCallback.run();
            }
        }
    }

}