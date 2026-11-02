/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.entity;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.world.level.entity.PersistentEntitySectionManagerInjection;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerInject<T extends EntityAccess> implements PersistentEntitySectionManagerInjection<T> {

    @Unique private final AtomicBoolean nytheriacompat$callWithoutEvent = new AtomicBoolean(false);

    @Override
    public void nytheriacompat$markWithoutEvent() {
        this.nytheriacompat$callWithoutEvent.set(true);
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$callEntityJoinLevelEvent(T entity, boolean worldGenSpawned, CallbackInfoReturnable<Boolean> cir) {
        // Yes, this should in fact be inverted. We want the event to be fired when nytheriacompat$callWithoutEvent is set to false.
        if (!this.nytheriacompat$callWithoutEvent.getAndSet(false) && entity instanceof Entity e && EventGate.has(EntityJoinLevelEvent.class) && NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(e, e.level(), worldGenSpawned)).isCanceled())
            cir.setReturnValue(false);
    }

}
