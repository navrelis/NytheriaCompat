/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.level.block.entity.AbstractFurnaceBlockEntityInjection;

import java.util.Map;
import java.util.function.ObjIntConsumer;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityInject implements AbstractFurnaceBlockEntityInjection {
    @Shadow
    private static boolean canBurn(RegistryAccess registryAccess, @Nullable RecipeHolder<?> recipeHolder, NonNullList<ItemStack> nonNullList, int i) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static boolean burn(RegistryAccess registryAccess, @Nullable RecipeHolder<?> recipeHolder, NonNullList<ItemStack> nonNullList, int i) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Inject(method = "add(Ljava/util/Map;Lnet/minecraft/world/level/ItemLike;I)V", at = @At("TAIL"))
    private static void nytheriacompat$appendItemToNytheriaCompatMap(Map<Item, Integer> map, ItemLike item, int burnTime, CallbackInfo ci) {
        AbstractFurnaceBlockEntityInjection.nytheriacompat$itemCookTimes.put(item, burnTime);
    }

    @Inject(method = "add(Ljava/util/Map;Lnet/minecraft/tags/TagKey;I)V", at = @At("TAIL"))
    private static void nytheriacompat$appendTagToNytheriaCompatMap(Map<Item, Integer> map, TagKey<Item> itemTag, int burnTime, CallbackInfo ci) {
        AbstractFurnaceBlockEntityInjection.nytheriacompat$tagCookTimes.put(itemTag, burnTime);
    }

    @Unique
    private RecipeType<? extends AbstractCookingRecipe> recipeType;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$storeRecipeType(BlockEntityType<?> type, BlockPos pos, BlockState blockState, RecipeType<? extends AbstractCookingRecipe> recipeType, CallbackInfo ci) {
        this.recipeType = recipeType;
    }

    @ModifyReturnValue(method = "getBurnDuration", at = @At("RETURN"))
    private int nytheriacompat$tryUseCustomFuel(int original, @Local(argsOnly = true) ItemStack stack) {
        if (original != 0) {
            return original;
        }
        return stack.getBurnTime(recipeType);
    }

    @ModifyReturnValue(method = "isFuel", at = @At("RETURN"))
    private static boolean nytheriacompat$checkIsCustomFuel(boolean original, @Local(argsOnly = true) ItemStack stack) {
        return original || stack.getBurnTime(null) > 0;
    }

    @CreateStatic
    private static void buildFuels(ObjIntConsumer<Either<Item, TagKey<Item>>> fuelConsumer) {
        AbstractFurnaceBlockEntityInjection.buildFuels(fuelConsumer);
    }

    @Unique
    private static final ThreadLocal<AbstractFurnaceBlockEntity> nytheriacompat$currentFurnace = ThreadLocal.withInitial(() -> null);

    @CreateStatic
    private static boolean canBurn(RegistryAccess registryAccess, @Nullable RecipeHolder<?> recipe, NonNullList<ItemStack> inventory, int maxStackSize, AbstractFurnaceBlockEntity furnace) {
        nytheriacompat$currentFurnace.set(furnace);
        var result = canBurn(registryAccess, recipe, inventory, maxStackSize);
        nytheriacompat$currentFurnace.remove();
        return result;
    }

    @Inject(method = {"canBurn", "burn"}, at = @At("HEAD"))
    private static void nytheriacompat$setupCurrentFurnaceParam(RegistryAccess registryAccess, RecipeHolder<?> recipe, NonNullList<ItemStack> inventory, int maxStackSize, CallbackInfoReturnable<Boolean> cir, @Share(value = "currentFurnace", namespace = "nytheria_compat") LocalRef<AbstractFurnaceBlockEntity> currentFurnace) {
        currentFurnace.set(nytheriacompat$currentFurnace.get());
    }

    @CreateStatic
    private static boolean burn(RegistryAccess registryAccess, @Nullable RecipeHolder<?> recipe, NonNullList<ItemStack> inventory, int maxStackSize, AbstractFurnaceBlockEntity furnace) {
        nytheriacompat$currentFurnace.set(furnace);
        var result = burn(registryAccess, recipe, inventory, maxStackSize);
        nytheriacompat$currentFurnace.remove();
        return result;
    }
}
