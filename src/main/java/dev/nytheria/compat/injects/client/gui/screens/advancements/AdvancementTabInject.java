// TRACKED HASH: 4b65780a21b72c6641617a99b2ca8d8e31521909
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.screens.advancements;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.*;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.client.gui.screens.advancements.AdvancementTabInjection;

@Mixin(AdvancementTab.class)
public class AdvancementTabInject implements AdvancementTabInjection {
    @Shadow @Final @Mutable private int index;
    @Unique private int page;

    public AdvancementTabInject(Minecraft p_97145_, AdvancementsScreen p_97146_, AdvancementTabType p_97147_, int p_97148_, AdvancementNode p_97149_, DisplayInfo p_97150_) {}

    @CreateInitializer
    public AdvancementTabInject(Minecraft mc, AdvancementsScreen screen, AdvancementTabType type, int index, int page, AdvancementNode adv, DisplayInfo info) {
        this(mc, screen, type, index, adv, info);
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    public void nytheriacompat$setPage(int page) {
        this.page = page;
    }

    @Override
    public void nytheriacompat$setIndex(int index) {
        this.index = index;
    }

}