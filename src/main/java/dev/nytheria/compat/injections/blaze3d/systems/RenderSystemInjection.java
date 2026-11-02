/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.blaze3d.systems;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.neoforge.client.GlStateBackup;
import dev.nytheria.compat.injections.blaze3d.platform.GlStateManagerInjection;

public interface RenderSystemInjection {
    static void backupGlState(GlStateBackup state) {
        RenderSystem.assertOnRenderThread();
        GlStateManagerInjection._backupGlState(state);
    }

    static void restoreGlState(GlStateBackup state) {
        RenderSystem.assertOnRenderThread();
        GlStateManagerInjection._restoreGlState(state);
    }
}
