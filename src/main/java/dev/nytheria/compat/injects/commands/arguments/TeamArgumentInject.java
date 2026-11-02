// TRACKED HASH: 3f27ff60749acdf7b5e6a9c9bad11c7389fffdf8
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.commands.arguments;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TeamArgument.class)
public abstract class TeamArgumentInject {
    @Inject(method = "getTeam", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;getPlayerTeam(Ljava/lang/String;)Lnet/minecraft/world/scores/PlayerTeam;"))
    private static void nytheriacompat$useSourceScoreboard(CommandContext<CommandSourceStack> context, String name, CallbackInfoReturnable<Objective> cir, @Local LocalRef<Scoreboard> scoreboard) {
        scoreboard.set(context.getSource().getScoreboard());
    }
}