// TRACKED HASH: 1c741d46a32ea4ff7443ab42f7ce23558940dd93
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.client.gui.screens.MenuScreensInjection;

import java.util.Optional;

@Mixin(MenuScreens.class)
public class MenuScreensInject implements MenuScreensInjection {
    @CreateStatic
    private static <T extends AbstractContainerMenu> Optional<MenuScreens.ScreenConstructor<T, ?>> getScreenFactory(MenuType<T> menuType) {
        return MenuScreensInjection.getScreenFactory(menuType);
    }
}