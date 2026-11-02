// TRACKED HASH: c0624f2fffa8e1e7c7a62d163e82c80147683857
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.data.loot;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.data.loot.LootTableProviderInjection;

import java.util.List;

@Mixin(LootTableProvider.class)
public class LootTableProviderInject implements LootTableProviderInjection {
    @Shadow @Final private List<LootTableProvider.SubProviderEntry> subProviders;

    @Override
    public List<LootTableProvider.SubProviderEntry> getTables() {
        return this.subProviders;
    }

}