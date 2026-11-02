/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity;

import dev.nytheria.compat.hook.EventGate;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbInject extends Entity {

    public ExperienceOrbInject(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Definition(id = "player", local = @Local(type = Player.class, argsOnly = true))
    @Definition(id = "takeXpDelay", field = "Lnet/minecraft/world/entity/player/Player;takeXpDelay:I")
    @Expression("player.takeXpDelay = ?")
    @Inject(method = "playerTouch", at = @At("MIXINEXTRAS:EXPRESSION"), cancellable = true)
    private void nytheriacompat$callPickupExpEvent(Player player, CallbackInfo ci) {
        if (EventGate.has(PlayerXpEvent.PickupXp.class) && NeoForge.EVENT_BUS.post(new PlayerXpEvent.PickupXp(player, (ExperienceOrb) (Object) this)).isCanceled())
            ci.cancel();
    }

    // Nytheria Compat TODO: missing stuff
}
