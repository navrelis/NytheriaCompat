// TRACKED HASH: 030f407a6354673d00123c0bcaa1c7f070d001e1
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.commands.arguments;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(ObjectiveArgument.class)
public abstract class ObjectiveArgumentInject {
    @Inject(method = "getObjective", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;getObjective(Ljava/lang/String;)Lnet/minecraft/world/scores/Objective;"))
    private static void nytheriacompat$useSourceScoreboard(CommandContext<CommandSourceStack> context, String name, CallbackInfoReturnable<Objective> cir, @Local LocalRef<Scoreboard> scoreboard) {
        scoreboard.set(context.getSource().getScoreboard());
    }

    @WrapOperation(method = "listSuggestions", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerScoreboard;getObjectiveNames()Ljava/util/Collection;"))
    private Collection<String> nytheriacompat$useSourceScoreboardNames(ServerScoreboard instance, Operation<Collection<String>> original, @Local CommandSourceStack source) {
        return source.getScoreboard().getObjectiveNames();
    }
}