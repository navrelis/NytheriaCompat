/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.server;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.ReloadableServerResources;
import net.neoforged.neoforge.common.conditions.ICondition;
import dev.nytheria.compat.processor.FabricInjectedInterface;

@FabricInjectedInterface(ReloadableServerResources.class)
public interface ReloadableServerResourcesInjection {
    default ICondition.IContext getConditionContext() {
        throw new RuntimeException("mixin, why didn't you add this");
    }

    default HolderLookup.Provider getRegistryLookup() {
        throw new RuntimeException("mixin, why didn't you add this");
    }
}