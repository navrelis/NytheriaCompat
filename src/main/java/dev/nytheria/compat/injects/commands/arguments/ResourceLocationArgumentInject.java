/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.commands.arguments;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.neoforged.neoforge.common.extensions.ICommandSourceStackExtension;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.world.item.crafting.RecipeManager;

@Mixin(ResourceLocationArgument.class)
public abstract class ResourceLocationArgumentInject {
    // Nytheria Compat: asked against ICommandSourceStackExtension, which declares getAdvancement and getRecipeManager as defaults (against CommandSourceStack.class they were true for every source).
    @Definition(id = "getAdvancements", method = "Lnet/minecraft/server/MinecraftServer;getAdvancements()Lnet/minecraft/server/ServerAdvancementManager;")
    @Definition(id = "get", method = "Lnet/minecraft/server/ServerAdvancementManager;get(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/advancements/AdvancementHolder;")
    @Expression("?.getAdvancements().get(?)")
    @WrapOperation(method = "getAdvancement", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static AdvancementHolder nytheriacompat$useSourceAdvancementGetter(ServerAdvancementManager instance, ResourceLocation location, Operation<AdvancementHolder> original, @Local(argsOnly = true) CommandContext<CommandSourceStack> ctx) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverrideWithReturnType(ctx.getSource().getClass(), ICommandSourceStackExtension.class, "getAdvancement", AdvancementHolder.class, ResourceLocation.class)) {
            return ctx.getSource().getAdvancement(location);
        }

        return original.call(instance, location);
    }

    @Definition(id = "getRecipeManager", method = "Lnet/minecraft/server/MinecraftServer;getRecipeManager()Lnet/minecraft/world/item/crafting/RecipeManager;")
    @Expression("?.getRecipeManager()")
    @WrapOperation(method = "getRecipe", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static RecipeManager nytheriacompat$useSourceRecipeManagerGetter(MinecraftServer instance, Operation<RecipeManager> original, @Local(argsOnly = true) CommandContext<CommandSourceStack> ctx) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverrideWithReturnType(ctx.getSource().getClass(), ICommandSourceStackExtension.class, "getRecipeManager", RecipeManager.class)) {
            return ctx.getSource().getRecipeManager();
        }

        return original.call(instance);
    }
}
