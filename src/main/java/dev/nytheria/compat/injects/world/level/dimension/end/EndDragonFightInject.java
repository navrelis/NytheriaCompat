/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.dimension.end;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.world.level.dimension.end.EndDragonFightInjection;

@Mixin(EndDragonFight.class)
public abstract class EndDragonFightInject implements EndDragonFightInjection {
    @Shadow @Final private ServerBossEvent dragonEvent;

    @Override
    public void addPlayer(ServerPlayer player) {
        this.dragonEvent.addPlayer(player);
    }

    @Override
    public void removePlayer(ServerPlayer player) {
        this.dragonEvent.removePlayer(player);
    }
}
