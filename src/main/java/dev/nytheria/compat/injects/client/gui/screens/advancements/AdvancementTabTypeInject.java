// TRACKED HASH: 3c5d5e9f66a3120c890c713989fdec4d4e81fe9f
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.screens.advancements;

import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.helpers.mixin.CreateStatic;

import java.util.Arrays;

@Mixin(AdvancementTabType.class)
public abstract class AdvancementTabTypeInject {
    @Shadow
    public static AdvancementTabType[] values() {
        throw new IllegalStateException();
    }

    @CreateStatic
    private static final int MAX_TABS = Arrays.stream(values()).mapToInt(AdvancementTabType::getMax).sum();
}