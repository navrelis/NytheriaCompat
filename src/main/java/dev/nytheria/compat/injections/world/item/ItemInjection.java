/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.function.Consumer;

public interface ItemInjection {
    default void initializeClient(Consumer<IClientItemExtensions> consumer) {
        throw NytheriaCompatHelper.createMixinException(ItemInjection.class, "initializeClient");
    }

    default void modifyDefaultComponentsFrom(DataComponentPatch patch) {
        throw NytheriaCompatHelper.createMixinException(ItemInjection.class, "modifyDefaultComponentsFrom");
    }

    interface TooltipContextInjection {
        default Level level() {
            throw NytheriaCompatHelper.createMixinException(ItemInjection.class, "level");
        }
    }
}
