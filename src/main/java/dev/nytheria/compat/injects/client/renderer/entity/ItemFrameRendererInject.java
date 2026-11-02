/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.entity;

import dev.nytheria.compat.hook.EventGate;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.neoforge.client.event.RenderItemInFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererInject<T extends ItemFrame> {
    @Definition(id = "mapId", local = @Local(type = MapId.class))
    @Expression("mapId != null")
    @WrapOperation(method = "render(Lnet/minecraft/world/entity/decoration/ItemFrame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 1))
    private boolean nytheriacompat$checkCanRenderFrame(Object left, Object right, Operation<Boolean> original, @Share("mapData") LocalRef<MapItemSavedData> mapData, @Local(argsOnly = true) ItemFrame frame, @Local(argsOnly = true) PoseStack poseStack, @Local(argsOnly = true) MultiBufferSource bufferSource, @Local(argsOnly = true) int packedLight, @Cancellable CallbackInfo ci) {
        if (EventGate.has(RenderItemInFrameEvent.class) && NeoForge.EVENT_BUS.post(new RenderItemInFrameEvent(frame, (ItemFrameRenderer<T>) (Object) this, poseStack, bufferSource, packedLight)).isCanceled()) {
            poseStack.popPose(); // We need to pop the pose so it closes correctly.
            ci.cancel();

            return false;
        }

        return original.call(left, right) || mapData.get() != null;
    }

}
