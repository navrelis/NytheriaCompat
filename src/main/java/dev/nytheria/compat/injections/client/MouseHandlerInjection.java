/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client;

import net.minecraft.client.MouseHandler;
import dev.nytheria.compat.processor.FabricInjectedInterface;

@FabricInjectedInterface(MouseHandler.class)
public interface MouseHandlerInjection {
    default double getXVelocity() {
        throw new IllegalStateException();
    }

    default double getYVelocity() {
        throw new IllegalStateException();
    }
}
