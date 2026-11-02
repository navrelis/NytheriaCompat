/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.gui.screens.inventory;

import net.minecraft.world.inventory.Slot;

import java.util.concurrent.atomic.AtomicInteger;

public interface AbstractContainerScreenInjection {
    AtomicInteger nytheriacompat$slotColor = new AtomicInteger(-2130706433);
    int defaultSlotColor = -2130706433;

    Slot getSlotUnderMouse();
    int getGuiLeft();
    int getGuiTop();
    int getSlotColor(int index);
    void nytheriacompat$setSlotColor(int slotColor);
}
