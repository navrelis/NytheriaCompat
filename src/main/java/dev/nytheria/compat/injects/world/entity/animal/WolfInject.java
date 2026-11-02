/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.animal;

import dev.nytheria.compat.hook.Scope;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Wolf.class)
public abstract class WolfInject extends TamableAnimal {
    protected WolfInject(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    // Nytheria Compat: I don't think we need to implement the game event?


    @Definition(id = "itemStack", local = @Local(type = ItemStack.class))
    @Definition(id = "is", method = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z")
    @Definition(id = "SHEARS", field = "Lnet/minecraft/world/item/Items;SHEARS:Lnet/minecraft/world/item/Item;")
    @Expression("itemStack.is(SHEARS)")
    @WrapOperation(method = "mobInteract", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$checkCanRemoveArmor(ItemStack instance, Item item, Operation<Boolean> original) {
        return original.call(instance, item) || (Scope.isNeoObject(instance.getItem()) && instance.canPerformAction(ItemAbilities.SHEARS_REMOVE_ARMOR));
    }

}
