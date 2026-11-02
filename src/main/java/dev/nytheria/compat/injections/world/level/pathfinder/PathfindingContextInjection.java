/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.pathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@FabricInjectedInterface(PathfindingContext.class)
public interface PathfindingContextInjection {
    default BlockPos currentEvalPos() {
        throw NytheriaCompatHelper.createMixinException(PathfindingContextInjection.class, "currentEvalPos");
    }
}
