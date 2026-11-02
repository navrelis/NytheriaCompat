/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.screens.packs;

import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.client.gui.screens.packs.PackSelectionModelEntryInjection;
import dev.nytheria.compat.injections.server.packs.repository.PackInjection;

@Mixin(PackSelectionModel.class)
public abstract class PackSelectionModelInject {
    @Mixin(PackSelectionModel.Entry.class)
    public interface EntryInject extends PackSelectionModelEntryInjection {
        @Override
        default boolean notHidden() {
            return true;
        }
    }

    @Mixin(targets = "net.minecraft.client.gui.screens.packs.PackSelectionModel$EntryBase")
    public abstract static class EntryBaseInject implements PackSelectionModelEntryInjection {
        @Shadow @Final private Pack pack;

        @Override
        public boolean notHidden() {
            return !((PackInjection) pack).isHidden();
        }
    }
}
