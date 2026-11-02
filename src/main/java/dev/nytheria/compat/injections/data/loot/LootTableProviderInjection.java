/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.data.loot;

import net.minecraft.data.loot.LootTableProvider;
import dev.nytheria.compat.processor.FabricInjectedInterface;

import java.util.List;

@FabricInjectedInterface(LootTableProvider.class)
public interface LootTableProviderInjection {
    default List<LootTableProvider.SubProviderEntry> getTables() {
        throw new IllegalStateException();
    }

}
