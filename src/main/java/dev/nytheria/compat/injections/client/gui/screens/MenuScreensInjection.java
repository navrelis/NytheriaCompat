/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.nytheria.compat.mixin.MenuScreensAccessor;

import java.util.Optional;

public interface MenuScreensInjection {
    static <T extends AbstractContainerMenu> Optional<MenuScreens.ScreenConstructor<T, ?>> getScreenFactory(MenuType<T> menuType) {
        var screenConstructor = MenuScreens.getConstructor(menuType);

        if (screenConstructor == null) {
            MenuScreensAccessor.nytheriacompat$getLogger().warn("Failed to create screen for menu type: {}", BuiltInRegistries.MENU.getKey(menuType));
        } else {
            return Optional.of(screenConstructor);
        }

        return Optional.empty();
    }

    @ApiStatus.Internal
    static void init() {
        var event = new RegisterMenuScreensEvent(MenuScreensAccessor.nytheriacompat$getScreens());
        ModLoader.postEvent(event);
    }
}
