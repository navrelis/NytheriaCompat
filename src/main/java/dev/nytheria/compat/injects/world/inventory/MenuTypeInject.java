// TRACKED HASH: 00999822b8acf90ce2c48fea9dad5cfee4bcdd4e
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.inventory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MenuType.class)
public abstract class MenuTypeInject<T extends AbstractContainerMenu> implements IMenuTypeExtension<T> {
    @Shadow @Final private MenuType.MenuSupplier<T> constructor;

    @Shadow public abstract T create(int containerId, Inventory playerInventory);

    @Override
    public T create(int windowId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        if (this.constructor instanceof IContainerFactory) {
            return ((IContainerFactory<T>) this.constructor).create(windowId, playerInv, extraData);
        }

        return this.create(windowId, playerInv);
    }
}