/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.inventory;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.inventory.BrewingStandMenuInjection;

@Mixin(BrewingStandMenu.class)
public abstract class BrewingStandMenuInject {
    @ModifyExpressionValue(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V", at = @At(value = "NEW", target = "(Lnet/minecraft/world/Container;III)Lnet/minecraft/world/inventory/BrewingStandMenu$PotionSlot;"))
    private static BrewingStandMenu.PotionSlot nytheriacompat$addPotionBrewing(BrewingStandMenu.PotionSlot original, @Local PotionBrewing brewing) {
        ((BrewingStandMenuInjection.PotionSlotInjection) original).nytheriacompat$setPotionBrewing(brewing);
        return original;
    }

    @Mixin(BrewingStandMenu.PotionSlot.class)
    public abstract static class PotionSlotInject implements BrewingStandMenuInjection.PotionSlotInjection {
        @Shadow public static boolean mayPlaceItem(ItemStack itemStack) {
            throw new IllegalStateException();
        }

        @Unique private PotionBrewing potionBrewing = PotionBrewing.EMPTY;
        @Unique private static final ThreadLocal<PotionBrewing> nytheriacompat$potionBrewing = ThreadLocal.withInitial(() -> PotionBrewing.EMPTY);

        public PotionSlotInject(Container container, int slot, int x, int y) {}

        @CreateInitializer
        public PotionSlotInject(PotionBrewing brewing, Container container, int slot, int x, int y) {
            this(container, slot, x, y);
            this.potionBrewing = brewing;
        }

        @WrapOperation(method = "mayPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BrewingStandMenu$PotionSlot;mayPlaceItem(Lnet/minecraft/world/item/ItemStack;)Z"))
        private boolean nytheriacompat$tryUseNeoMayPlaceItem(ItemStack itemStack, Operation<Boolean> original) {
            nytheriacompat$potionBrewing.set(this.potionBrewing);
            var result = original.call(itemStack);
            nytheriacompat$potionBrewing.remove();
            return result;
        }

        @ModifyReturnValue(method = "mayPlaceItem", at = @At("RETURN"))
        private static boolean nytheriacompat$tryUseBrewingResult(boolean original, @Local(argsOnly = true) ItemStack stack) {
            return nytheriacompat$potionBrewing.get().isInput(stack) || original;
        }

        @CreateStatic
        private static boolean mayPlaceItem(PotionBrewing potionBrewing, ItemStack stack) {
            nytheriacompat$potionBrewing.set(potionBrewing);
            var result = mayPlaceItem(stack);
            nytheriacompat$potionBrewing.remove();
            return result;
        }

        @Override
        public void nytheriacompat$setPotionBrewing(PotionBrewing brewing) {
            potionBrewing = brewing;
        }
    }
}
