/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.multiplayer.prediction;

import net.neoforged.neoforge.common.util.BlockSnapshot;
import dev.nytheria.compat.util.NytheriaCompatHelper;


public interface BlockStatePredictionHandlerInjection {
    interface ServerVerifiedStateInjection {
        default BlockSnapshot nytheriacompat$getSnapshot() {
            throw NytheriaCompatHelper.createMixinException(BlockStatePredictionHandlerInjection.class, "nytheriacompat$getSnapshot");
        }

    }
}
