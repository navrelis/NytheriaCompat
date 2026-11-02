// TRACKED HASH: 8a008dde196be8f110c6df462a387035cbfd879c
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.IMinecraftExtension;
import net.neoforged.neoforge.client.loading.ClientModLoader;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.client.MinecraftInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

@Mixin(Minecraft.class)
public abstract class MinecraftInject implements MinecraftInjection, IMinecraftExtension {
    @Shadow @Final @Mutable private ItemColors itemColors;
    @Shadow @Final private ReloadableResourceManager resourceManager;
    @Shadow @Final public Options options;
    @Shadow @Final public ParticleEngine particleEngine;
    @Shadow @Final private PackRepository resourcePackRepository;
    @Shadow public abstract BlockColors getBlockColors();
    @Shadow @Nullable public LocalPlayer player;
    @Shadow @Nullable public HitResult hitResult;
    @Shadow @Nullable public ClientLevel level;
    @Shadow @Nullable public MultiPlayerGameMode gameMode;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;reload()V"), method = "<init>")
    public void nytheriacompat$initializeClientModLoader(GameConfig gameConfig, CallbackInfo ci) {
        ClientModLoader.begin((Minecraft) (Object) this, this.resourcePackRepository, this.resourceManager);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateVsync(Z)V", shift = At.Shift.BEFORE), method = "<init>")
    public void nytheriacompat$initializeForge(GameConfig gameConfig, CallbackInfo ci) {
        ClientHooks.initClientHooks((Minecraft) (Object) this, this.resourceManager);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;<init>(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/renderer/texture/TextureManager;)V", shift = At.Shift.BY, by = 2))
    private void nytheriacompat$postRegisterParticleProviders(GameConfig gameConfig, CallbackInfo ci) {
        ClientHooks.onRegisterParticleProviders(this.particleEngine);
    }

    @ModifyReturnValue(method = "buildInitialScreens", at = @At("RETURN"))
    private Runnable nytheriacompat$finishModLoading(Runnable original) {;
        return ClientModLoader.completeModLoading(original);
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 1))
    private void rightClickAir(CallbackInfo ci, @Local ItemStack stack, @Local InteractionHand hand) {
        if (stack.isEmpty() && (this.hitResult == null || this.hitResult.getType() == HitResult.Type.MISS)) {
            CommonHooks.onEmptyClick(this.player, hand);
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void nytheriacompat$tryHandleUnloadLevel(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        if (this.level != null) {
            if (EventGate.has(LevelEvent.Unload.class)) NeoForge.EVENT_BUS.post(new LevelEvent.Unload(this.level));
        }
    }

    @Definition(id = "gameMode", field = "Lnet/minecraft/client/Minecraft;gameMode:Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;")
    @Expression("this.gameMode = null")
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void nytheriacompat$firePlayerLogoutEvent(Screen nextScreen, boolean keepResourcePacks, CallbackInfo ci) {
        ClientHooks.firePlayerLogout(this.gameMode, this.player);
    }

    // Nytheria Compat: we're not reverting registries thanks

    @Inject(method = "clearClientLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void nytheriacompat$callUnloadEventInClear(Screen nextScreen, CallbackInfo ci) {
        if (this.level != null) {
            if (EventGate.has(LevelEvent.Unload.class)) NeoForge.EVENT_BUS.post(new LevelEvent.Unload(this.level));
        }
    }

    @Definition(id = "integratedServer", local = @Local(type = IntegratedServer.class))
    @Expression("integratedServer != null")
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void nytheriacompat$handleUnloadEvent(Screen nextScreen, boolean keepResourcePacks, CallbackInfo ci) {
        if (EventGate.has(LevelEvent.Unload.class)) NeoForge.EVENT_BUS.post(new LevelEvent.Unload(this.level));
    }

    @WrapOperation(method = "pickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack nytheriacompat$tryUseForgeCloneItemStack(Block instance, LevelReader levelReader, BlockPos blockPos, BlockState blockState, Operation<ItemStack> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(instance.getClass(), IBlockExtension.class, "getCloneItemStack", BlockState.class, HitResult.class, LevelReader.class, BlockPos.class, Player.class)) {
            return instance.getCloneItemStack(blockState, this.hitResult, levelReader, blockPos, this.player);
        }

        return original.call(instance, levelReader, blockPos, blockState);
    }

    @WrapOperation(method = "pickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getPickResult()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack nytheriacompat$tryUseForgePickedResult(Entity instance, Operation<ItemStack> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(instance.getClass(), IEntityExtension.class, "getPickedResult", HitResult.class)) {
            return instance.getPickedResult(this.hitResult);
        }

        var result = original.call(instance);

        if (result == null) {
            SpawnEggItem egg = DeferredSpawnEggItem.deferredOnlyById(instance.getType());
            if (egg != null)
                result = new ItemStack(egg);
            else
                result = ItemStack.EMPTY;
        }

        return result;
    }

    @Override
    public ItemColors getItemColors() {
        if (this.itemColors == null)
            this.itemColors = ItemColors.createDefault(this.getBlockColors());

        return this.itemColors;
    }
}
