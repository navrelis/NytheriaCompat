/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on NeoForge by NeoForged, as ported by KiltMC (see VENDORED.md). Licensed under the GNU LGPL 2.1.
 */

package net.neoforged.neoforge.event.entity.living;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus;

public abstract class LivingSwapItemsEvent extends LivingEvent {
    @ApiStatus.Internal
    public LivingSwapItemsEvent(LivingEntity entity) {
        super(entity);
    }

    /**
     * This event is fired when a living entity is about to swap the items in their main and offhand.
     * This event is executed in {@link ServerGamePacketListenerImpl#handlePlayerAction}
     *
     * <p>This event is {@linkplain ICancellableEvent cancellable}, and does not {@linkplain HasResult have a result}.
     */
    public static class Hands extends LivingSwapItemsEvent implements ICancellableEvent {
        private ItemStack toMainHand;
        private ItemStack toOffHand;

        @ApiStatus.Internal
        public Hands(LivingEntity entity) {
            super(entity);
            this.toMainHand = entity.getOffhandItem(); //the main hand will be swapped with the offhand
            this.toOffHand = entity.getMainHandItem(); //the offhand will be swapped with the main hand
        }

        // Nytheria Compat: Pass the stacks to the event directly.
        @ApiStatus.Internal
        public Hands(LivingEntity entity, ItemStack toMainHand, ItemStack toOffHand) {
            super(entity);
            this.toMainHand = toMainHand;
            this.toOffHand = toOffHand;
        }
        // Nytheria Compat end

        /**
         * Gets the item that will be swapped to the main hand of the entity.
         * 
         * @return The item that will be swapped to the main hand.
         */
        public ItemStack getItemSwappedToMainHand() {
            return toMainHand;
        }

        /**
         * Gets the item that will be swapped to the offhand of the entity.
         * 
         * @return The item that will be swapped to the offhand.
         */
        public ItemStack getItemSwappedToOffHand() {
            return toOffHand;
        }

        /**
         * Sets the item that will be swapped to the main hand of the entity.
         * 
         * @param item The item to swap to the main hand.
         */
        public void setItemSwappedToMainHand(ItemStack item) {
            this.toMainHand = item;
        }

        /**
         * Sets the item that will be swapped to the offhand of the entity.
         * 
         * @param item The item to swap to the offhand.
         */
        public void setItemSwappedToOffHand(ItemStack item) {
            this.toOffHand = item;
        }
    }
}
