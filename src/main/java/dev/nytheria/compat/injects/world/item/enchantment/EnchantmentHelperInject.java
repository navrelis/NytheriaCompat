/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item.enchantment;

import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.item.enchantment.EnchantmentHelperInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperInject implements EnchantmentHelperInjection {
    // Nytheria Compat: these checks asked against Item.class, but the methods are defaults of IItemExtension (which Item gets
    //  through interface injection), so they were true for every item and the hooks below replaced vanilla code (and
    //  every other mod's mixin into it) even for plain vanilla items. Asked against IItemExtension, they are only true
    //  for an item class that really overrides the method.
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$GET_ENCHANTMENT_LEVEL_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IItemExtension.class, "getEnchantmentLevel", ItemStack.class, Holder.class);
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$GET_ALL_ENCHANTMENTS_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IItemExtension.class, "getAllEnchantments", ItemStack.class, HolderLookup.RegistryLookup.class);

    @Inject(method = "getItemEnchantmentLevel", at = @At("HEAD"), cancellable = true)
    private static void nytheriacompat$tryHandleDirectEnchantmentLevel(Holder<Enchantment> enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (nytheriacompat$GET_ENCHANTMENT_LEVEL_OVERRIDE.test(stack.getItem().getClass()) && !EnchantmentHelperInjection.nytheriacompat$shouldUseTagEnchantment.get()) {
            cir.setReturnValue(stack.getEnchantmentLevel(enchantment));
        }
    }

    // Nytheria Compat: for an item that does not override getEnchantmentLevel the vanilla body runs (it reads the same
    //  enchantments component as the IItemExtension default); NeoForge's GetEnchantmentLevelEvent still gets to change
    //  its result, as IItemStackExtension#getEnchantmentLevel would, but only if a listener exists (nothing is built otherwise).
    @ModifyReturnValue(method = "getItemEnchantmentLevel", at = @At("RETURN"))
    private static int nytheriacompat$postEnchantmentLevelEvent(int original, Holder<Enchantment> enchantment, ItemStack stack) {
        if (!EventGate.has(GetEnchantmentLevelEvent.class) || nytheriacompat$GET_ENCHANTMENT_LEVEL_OVERRIDE.test(stack.getItem().getClass()) || EnchantmentHelperInjection.nytheriacompat$shouldUseTagEnchantment.get()) {
            return original;
        }

        return EventHooks.getEnchantmentLevelSpecific(original, stack, enchantment);
    }

    @CreateStatic
    private static int getTagEnchantmentLevel(Holder<Enchantment> enchantment, ItemStack stack) {
        EnchantmentHelperInjection.nytheriacompat$shouldUseTagEnchantment.set(true);
        var value = EnchantmentHelperInjection.getTagEnchantmentLevel(enchantment, stack);
        EnchantmentHelperInjection.nytheriacompat$shouldUseTagEnchantment.remove();
        return value;
    }

    @ModifyVariable(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/ItemEnchantments;entrySet()Ljava/util/Set;"))
    private static ItemEnchantments nytheriacompat$tryGetAllEnchantmentsFromLookup(ItemEnchantments value, @Local(argsOnly = true) ItemStack stack) {
        boolean override = nytheriacompat$GET_ALL_ENCHANTMENTS_OVERRIDE.test(stack.getItem().getClass());
        if (!override && !EventGate.has(GetEnchantmentLevelEvent.class)) {
            return value;
        }

        var lookup = CommonHooks.resolveLookup(Registries.ENCHANTMENT);
        if (lookup != null) {
            if (override) {
                return stack.getAllEnchantments(lookup);
            } else {
                return EventHooks.getAllEnchantmentLevels(value, stack, lookup); // Nytheria Compat: Make sure to still call the event.
            }
        }

        return value;
    }

    @Definition(id = "itemEnchantments", local = @Local(type = ItemEnchantments.class))
    @Expression("itemEnchantments != null")
    @ModifyVariable(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static ItemEnchantments nytheriacompat$tryGetAllEnchantments(ItemEnchantments value, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) LivingEntity entity) {
        boolean override = nytheriacompat$GET_ALL_ENCHANTMENTS_OVERRIDE.test(stack.getItem().getClass());
        if (!override && !EventGate.has(GetEnchantmentLevelEvent.class)) {
            return value;
        }

        var lookup = entity.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        if (override) {
            return stack.getAllEnchantments(lookup);
        } else {
            return EventHooks.getAllEnchantmentLevels(value, stack, lookup); // Nytheria Compat: Make sure to still call the event.
        }
    }

}
