// TRACKED HASH: 0352f8e699c8b1e9b6ac77b9927e3852ffaf311c
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.client.renderer.ScreenEffectRendererInjection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererInject implements ScreenEffectRendererInjection {

    @Unique private static @Nullable BlockPos nytheriacompat$overlayBlockPos = null;

    @WrapOperation(method = "renderScreenEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockModelShaper;getParticleIcon(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private static TextureAtlasSprite nytheriacompat$tryUseCustomOverlayTexture(BlockModelShaper instance, BlockState state, Operation<TextureAtlasSprite> original, @Local(argsOnly = true) Minecraft minecraft) {
        if (nytheriacompat$overlayBlockPos != null && minecraft.level != null) {
            var data = minecraft.level.getModelDataManager().getAt(nytheriacompat$overlayBlockPos);
            var model = instance.getBlockModel(state);

            if (model.getModelData(minecraft.level, nytheriacompat$overlayBlockPos, state, data == null ? ModelData.EMPTY : data) != ModelData.EMPTY) {
                return instance.getTexture(state, minecraft.level, nytheriacompat$overlayBlockPos);
            }
        }

        return original.call(instance, state);
    }

    @Definition(id = "minecraft", local = @Local(type = Minecraft.class, argsOnly = true))
    @Definition(id = "player", field = "Lnet/minecraft/client/Minecraft;player:Lnet/minecraft/client/player/LocalPlayer;")
    @Definition(id = "isEyeInFluid", method = "Lnet/minecraft/client/player/LocalPlayer;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z")
    @Definition(id = "WATER", field = "Lnet/minecraft/tags/FluidTags;WATER:Lnet/minecraft/tags/TagKey;")
    @Expression("minecraft.player.isEyeInFluid(WATER)")
    @ModifyExpressionValue(method = "renderScreenEffect", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean nytheriacompat$tryRenderCustomFluidOverlay(boolean original, @Local Player player, @Local(argsOnly = true) Minecraft mc, @Local(argsOnly = true) PoseStack poseStack) {
        if (!original) {
            if (!player.getEyeInFluidType().isAir()) {
                IClientFluidTypeExtensions.of(player.getEyeInFluidType())
                    .renderOverlay(mc, poseStack);
            }

            return false;
        }

        return true;
    }

    @Inject(method = "getViewBlockingState", at = @At(value = "RETURN", ordinal = 0))
    private static void nytheriacompat$trySetCurrentBlockPos(Player player, CallbackInfoReturnable<BlockState> cir, @Local BlockPos.MutableBlockPos pos) {
        nytheriacompat$overlayBlockPos = pos.immutable();
    }

}
