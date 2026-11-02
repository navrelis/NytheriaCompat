// TRACKED HASH: 9169c1a016aa79fe41a22e24efb28b87a97545d4
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.screens.inventory;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.world.inventory.SlotInjection;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

@Mixin(value = CreativeModeInventoryScreen.class, priority = 1009)
public abstract class CreativeModeInventoryScreenInject extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    public CreativeModeInventoryScreenInject(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

//    Creative Tab sorting
//    @TargetHandler(
//            mixin = "net.fabricmc.fabric.mixin.itemgroup.client.CreativeInventoryScreenMixin",
//            name = "fabric_updateSelection"
//    )
//    @Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTabs;allTabs()Ljava/util/List;"))
//    private List<CreativeModeTab> nytheriacompat$allTabs() {
//        return CreativeModeTabRegistry.getSortedCreativeModeTabs();
//    }
//
//    @TargetHandler(
//            mixin = "net.fabricmc.fabric.mixin.itemgroup.client.CreativeInventoryScreenMixin",
//            name = "fabric_hasGroupForPage"
//    )
//    @Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTabs;tabs()Ljava/util/List;"))
//    private static List<CreativeModeTab> nytheriacompat$tabs() {
//        return CreativeModeTabRegistry.getSortedCreativeModeTabs();
//    }

    @Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
    public static class SlotWrapperInject implements SlotInjection {
        @Shadow @Final
        Slot target;

        @Override
        public int getSlotIndex() {
            return this.target.getSlotIndex();
        }

        @Override
        public boolean isSameInventory(Slot other) {
            return ((SlotInjection) this.target).isSameInventory(other);
        }

        @Override
        public Slot setBackground(ResourceLocation atlas, ResourceLocation sprite) {
            return this.target.setBackground(atlas, sprite);
        }
    }
}