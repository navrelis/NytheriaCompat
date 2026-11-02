/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.inventory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GrindstoneEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.world.inventory.GrindstoneMenuInjection;
import dev.nytheria.compat.mixin.world.inventory.GrindstoneMenuAccessor;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuInject extends AbstractContainerMenu implements GrindstoneMenuInjection {
    @Unique private int xp = -1;

    protected GrindstoneMenuInject(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Override
    public int nytheriacompat$getXp() {
        return xp;
    }

    @Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
    public abstract static class AnonymousResultSlotInject {
        @Shadow @Final private GrindstoneMenu field_16780;

        @Shadow protected abstract int getExperienceAmount(Level level);

        /** Nytheria Compat: the XP the OnTakeItem listeners saw, for the vanilla body of the current onTake call; -1 if none. */
        @Unique private int nytheriacompat$takeXp = -1;

        // Nytheria Compat: this used to call CommonHooks.onGrindstoneTake, which always returns true, so vanilla's onTake (and
        //  every other mod's mixin into it) never ran, also when no NeoForge mod listens to the event. Now the event is
        //  posted here, and the NeoForge result replaces vanilla's only when a listener cancelled the event or changed
        //  what it would do. Otherwise vanilla's body runs, which does exactly what NeoForge's default does (award the
        //  XP, play the sound, empty both inputs), with the XP amount the listeners saw.
        @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
        private void nytheriacompat$checkGrindstoneEvent(Player player, ItemStack stack, CallbackInfo ci) {
            Container inputSlots = ((GrindstoneMenuAccessor) field_16780).getRepairSlots();
            ContainerLevelAccess access = ((GrindstoneMenuAccessor) field_16780).getAccess();
            this.nytheriacompat$takeXp = -1;
            if (!EventGate.has(GrindstoneEvent.OnTakeItem.class)) return;

            boolean takeOver = access.evaluate((level, pos) -> {
                int xp = this.getExperienceAmount(level);
                GrindstoneEvent.OnTakeItem event = new GrindstoneEvent.OnTakeItem(access, player, inputSlots.getItem(0), inputSlots.getItem(1), xp);

                if (EventGate.has(GrindstoneEvent.OnTakeItem.class) && NeoForge.EVENT_BUS.post(event).isCanceled()) {
                    return true;
                }

                if (event.getXp() == xp && event.getNewTopItem().isEmpty() && event.getNewBottomItem().isEmpty()) {
                    this.nytheriacompat$takeXp = xp;
                    return false;
                }

                // The rest of CommonHooks.onGrindstoneTake.
                if (level instanceof ServerLevel serverLevel) {
                    ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), event.getXp());
                }
                level.levelEvent(1042, pos, 0);
                inputSlots.setItem(0, event.getNewTopItem());
                inputSlots.setItem(1, event.getNewBottomItem());
                inputSlots.setChanged();
                return true;
            }, false);

            if (takeOver)
                ci.cancel();
        }

        @Inject(method = "onTake", at = @At("TAIL"))
        private void nytheriacompat$clearTakeXp(Player player, ItemStack stack, CallbackInfo ci) {
            this.nytheriacompat$takeXp = -1;
        }

        @Inject(method = "getExperienceAmount", at = @At("HEAD"), cancellable = true)
        private void nytheriacompat$useStoredXp(Level level, CallbackInfoReturnable<Integer> cir) {
            if (this.nytheriacompat$takeXp > -1)
                cir.setReturnValue(this.nytheriacompat$takeXp);
            else if (((GrindstoneMenuInjection) field_16780).nytheriacompat$getXp() > -1)
                cir.setReturnValue(((GrindstoneMenuInjection) field_16780).nytheriacompat$getXp());
        }
    }
}
