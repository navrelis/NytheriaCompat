/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.server.level;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.OptionalInt;
import java.util.function.Consumer;

public interface ServerPlayerInjection {
    default String getLanguage() {
        throw NytheriaCompatHelper.createMixinException(ServerPlayerInjection.class, "getLanguage");
    }

    default Component getTabListHeader() {
        throw NytheriaCompatHelper.createMixinException(ServerPlayerInjection.class, "getTabListHeader");
    }

    default void setTabListHeader(Component header) {
        throw NytheriaCompatHelper.createMixinException(ServerPlayerInjection.class, "setTabListHeader");
    }

    default Component getTabListFooter() {
        throw NytheriaCompatHelper.createMixinException(ServerPlayerInjection.class, "getTabListFooter");
    }

    default void setTabListFooter(Component footer) {
        throw NytheriaCompatHelper.createMixinException(ServerPlayerInjection.class, "setTabListFooter");
    }

    default void setTabListHeaderFooter(Component header, Component footer) {
        throw NytheriaCompatHelper.createMixinException(ServerPlayerInjection.class, "setTabListHeaderFooter");
    }
}
