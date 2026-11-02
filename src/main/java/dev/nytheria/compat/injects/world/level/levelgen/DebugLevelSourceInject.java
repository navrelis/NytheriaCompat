// TRACKED HASH: 548c719285d4e2bcf4ee2fbb1ec72b4e0c47a89d
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.levelgen;

import net.minecraft.world.level.levelgen.DebugLevelSource;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.level.levelgen.DebugLevelSourceInjection;

@Mixin(DebugLevelSource.class)
public class DebugLevelSourceInject implements DebugLevelSourceInjection {
    @CreateStatic
    private static void initValidStates() {
        DebugLevelSourceInjection.initValidStates();
    }
}