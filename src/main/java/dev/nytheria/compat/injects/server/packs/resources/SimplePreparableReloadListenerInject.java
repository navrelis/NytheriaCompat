/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.packs.resources;

import io.github.fabricators_of_create.porting_lib.resources.conditions.ICondition;
import io.github.fabricators_of_create.porting_lib.resources.extensions.ContextAwareReloadListenerExtension;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import org.spongepowered.asm.mixin.*;
import dev.nytheria.compat.helpers.mixin.Extends;
import dev.nytheria.compat.workarounds.WrappedFabricConditionContext;
import dev.nytheria.compat.workarounds.WrappedNeoConditionContext;
import dev.nytheria.compat.workarounds.ContextAwareReloadListenerWorkaround;

@Extends(ContextAwareReloadListener.class)
@Mixin(SimplePreparableReloadListener.class)
public abstract class SimplePreparableReloadListenerInject implements ContextAwareReloadListenerWorkaround, ContextAwareReloadListenerExtension {
    // Nytheria Compat: Handle Porting Lib's contexts ourselves
    public ICondition.IContext port_lib$getContext() {
        return new WrappedNeoConditionContext(this.nytheriacompat$asContextAware().getContext());
    }

    @Override
    public void injectContext(ICondition.IContext context, HolderLookup.Provider registryLookup) {
        this.nytheriacompat$asContextAware().injectContext(new WrappedFabricConditionContext(context), registryLookup);
    }
}
