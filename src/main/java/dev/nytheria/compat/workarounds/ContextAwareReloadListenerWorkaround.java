/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds;

import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;

public interface ContextAwareReloadListenerWorkaround {
    default ContextAwareReloadListener nytheriacompat$asContextAware() {
        return (ContextAwareReloadListener) this;
    }

    default <T> ConditionalOps<T> nytheriacompat$makeConditionalOps(RegistryOps<T> original) {
        return new ConditionalOps<>(original, this.nytheriacompat$asContextAware().getContext());
    }
}
