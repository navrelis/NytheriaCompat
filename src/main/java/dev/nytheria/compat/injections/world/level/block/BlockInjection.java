/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.block;

import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.function.Consumer;

public interface BlockInjection {
    default void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        throw NytheriaCompatHelper.createMixinException(BlockInjection.class, "initializeClient");
    }
}
