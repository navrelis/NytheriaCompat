/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item.crafting;


import com.google.gson.Gson;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.workarounds.ContextAwareReloadListenerWorkaround;

import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.item.crafting.RecipeManager;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerInject extends SimpleJsonResourceReloadListener implements ContextAwareReloadListenerWorkaround {

    public RecipeManagerInject(Gson gson, String directory) {
        super(gson, directory);
    }
}
