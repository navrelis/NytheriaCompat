/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.entity.layers;

import java.util.concurrent.atomic.AtomicBoolean;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.NytheriaCompat;
import dev.nytheria.compat.injections.client.renderer.entity.layers.HumanoidArmorLayerInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;
import dev.nytheria.compat.workarounds.RenderLayerData;
import dev.nytheria.compat.workarounds.WrappedModelAsHumanoid;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;

@Implements(@Interface(iface = HumanoidArmorLayerInjection.class, prefix = "nytheriacompat$i$"))
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerInject<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
    @Shadow @Final private TextureAtlas armorTrimAtlas;
    @Shadow protected abstract void renderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, T livingEntity, EquipmentSlot slot, int packedLight, A model);

    @Unique private final RenderLayerData nytheriacompat$renderLayerData = new RenderLayerData();
    @Unique private final WrappedModelAsHumanoid nytheriacompat$wrappedHumanoidModel = new WrappedModelAsHumanoid();
    @Unique private final AtomicBoolean nytheriacompat$isRunningCompatibility = new AtomicBoolean(false);

    // Nytheria Compat: these run for every armor piece of every entity in every frame, resolve the override checks only once.
    //  getArmorModelHook is protected, so ofDeclared: Class.getMethod (what "of" uses) only finds public methods and would never see an override.
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$GET_ARMOR_MODEL_HOOK_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.ofDeclared(HumanoidArmorLayer.class, "getArmorModelHook", LivingEntity.class, ItemStack.class, EquipmentSlot.class, HumanoidModel.class);
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$GET_ARMOR_TEXTURE_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.withReturnType(IItemExtension.class, "getArmorTexture", ResourceLocation.class, ItemStack.class, Entity.class, EquipmentSlot.class, ArmorMaterial.Layer.class, boolean.class);

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"))
    private void nytheriacompat$updateRenderLayerData(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        this.nytheriacompat$renderLayerData.update(limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
    }

    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V"))
    private void nytheriacompat$tryHandleRenderArmorPieceCompatibility(HumanoidArmorLayer<T, M, A> instance, PoseStack poseStack, MultiBufferSource bufferSource, T livingEntity, EquipmentSlot slot, int packedLight, A model, Operation<Void> original) {
        this.nytheriacompat$isRunningCompatibility.set(true);
        var renderData = this.nytheriacompat$renderLayerData;

        // this is probably one of the dumber things I've done but hey I mean, it works
        ((HumanoidArmorLayer<T, M, A>) (Object) this).renderArmorPiece(poseStack, bufferSource, livingEntity, slot, packedLight, model, renderData.getLimbSwing(), renderData.getLimbSwingAmount(), renderData.getPartialTicks(), renderData.getAgeInTicks(), renderData.getNetHeadYaw(), renderData.getHeadPitch());

        // Nytheria Compat: If this passes, that means it never got to run, which means we should cancel it.
        if (this.nytheriacompat$isRunningCompatibility.getAndSet(false)) {
            return;
        }

        original.call(instance, poseStack, bufferSource, livingEntity, slot, packedLight, model);
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private void nytheriacompat$initShareData(PoseStack poseStack, MultiBufferSource bufferSource, T livingEntity, EquipmentSlot slot, int packedLight, A model, CallbackInfo ci,
                                    @Share(value = "limbSwing", namespace = NytheriaCompat.MOD_ID) LocalFloatRef limbSwing,
                                    @Share(value = "limbSwingAmount", namespace = NytheriaCompat.MOD_ID) LocalFloatRef limbSwingAmount,
                                    @Share(value = "partialTicks", namespace = NytheriaCompat.MOD_ID) LocalFloatRef partialTicks,
                                    @Share(value = "ageInTicks", namespace = NytheriaCompat.MOD_ID) LocalFloatRef ageInTicks,
                                    @Share(value = "netHeadYaw", namespace = NytheriaCompat.MOD_ID) LocalFloatRef netHeadYaw,
                                    @Share(value = "headPitch", namespace = NytheriaCompat.MOD_ID) LocalFloatRef headPitch) {
        limbSwing.set(this.nytheriacompat$renderLayerData.getLimbSwing());
        limbSwingAmount.set(this.nytheriacompat$renderLayerData.getLimbSwingAmount());
        partialTicks.set(this.nytheriacompat$renderLayerData.getPartialTicks());
        ageInTicks.set(this.nytheriacompat$renderLayerData.getAgeInTicks());
        netHeadYaw.set(this.nytheriacompat$renderLayerData.getNetHeadYaw());
        headPitch.set(this.nytheriacompat$renderLayerData.getHeadPitch());
    }

    @Inject(method = "renderArmorPiece", at= @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V", shift = At.Shift.AFTER))
    private void nytheriacompat$storeCustomArmorModel(PoseStack poseStack, MultiBufferSource bufferSource, T livingEntity, EquipmentSlot slot, int packedLight, A model, CallbackInfo ci, @Share(value = "model", namespace = NytheriaCompat.MOD_ID) LocalRef<Model> modelRef, @Local ItemStack stack) {
        modelRef.set(this.getArmorModelHook(livingEntity, stack, slot, model));
    }

    @ModifyVariable(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ArmorMaterial;layers()Ljava/util/List;"), ordinal = 1)
    private int nytheriacompat$setupCustomDefaultDyeColor(int original, PoseStack poseStack, MultiBufferSource bufferSource, T livingEntity, EquipmentSlot slot, int packedLight, A model, @Local ItemStack stack, @Share(value = "extensions", namespace = NytheriaCompat.MOD_ID) LocalRef<IClientItemExtensions> extensionsRef, @Share(value = "model", namespace = NytheriaCompat.MOD_ID) LocalRef<Model> modelRef) {
        extensionsRef.set(IClientItemExtensions.of(stack));
        if (extensionsRef.get() == IClientItemExtensions.DEFAULT)
            return original;

        extensionsRef.get().setupModelAnimations(livingEntity, stack, slot, modelRef.get(), this.nytheriacompat$renderLayerData.getLimbSwing(), this.nytheriacompat$renderLayerData.getLimbSwingAmount(), this.nytheriacompat$renderLayerData.getPartialTicks(), this.nytheriacompat$renderLayerData.getAgeInTicks(), this.nytheriacompat$renderLayerData.getNetHeadYaw(), this.nytheriacompat$renderLayerData.getHeadPitch());
        return extensionsRef.get().getDefaultDyeColor(stack);
    }

    @WrapOperation(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/HumanoidModel;ILnet/minecraft/resources/ResourceLocation;)V"))
    private void nytheriacompat$tryRenderModel(HumanoidArmorLayer<T, M, A> instance, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, A humanoidModel, int dyeColor, ResourceLocation resourceLocation, Operation<Void> original, @Share(value = "model", namespace = NytheriaCompat.MOD_ID) LocalRef<Model> modelRef, @Share(value = "extensions", namespace = NytheriaCompat.MOD_ID) LocalRef<IClientItemExtensions> extensionsRef, @Local ItemStack stack, @Local(argsOnly = true) LivingEntity entity, @Local ArmorMaterial.Layer layer, @Local ArmorMaterial material, @Local boolean flag, @Local(argsOnly = true) EquipmentSlot slot) {
        if (extensionsRef.get() != IClientItemExtensions.DEFAULT
            || nytheriacompat$GET_ARMOR_MODEL_HOOK_OVERRIDE.test(this.getClass())
            || nytheriacompat$GET_ARMOR_TEXTURE_OVERRIDE.test(stack.getItem().getClass())
        ) {
            int color = extensionsRef.get().getArmorLayerTintColor(stack, entity, layer, material.layers().indexOf(layer), dyeColor);

            if (color != 0) {
                this.nytheriacompat$wrappedHumanoidModel.getWrapped().set(modelRef.get());
                var texture = ClientHooks.getArmorTexture(entity, stack, layer, flag, slot);
                original.call(instance, poseStack, bufferSource, packedLight, this.nytheriacompat$wrappedHumanoidModel, color, texture);
            }
        } else {
            original.call(instance, poseStack, bufferSource, packedLight, humanoidModel, dyeColor, resourceLocation);
        }
    }

    @WrapOperation(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderTrim(Lnet/minecraft/core/Holder;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/HumanoidModel;Z)V"))
    private void nytheriacompat$tryUseWrapperForTrim(HumanoidArmorLayer<T, M, A> instance, Holder<ArmorMaterial> holder, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, ArmorTrim armorTrim, A humanoidModel, boolean bl, Operation<Void> original, @Share(value = "model", namespace = NytheriaCompat.MOD_ID) LocalRef<Model> modelRef, @Share(value = "extensions", namespace = NytheriaCompat.MOD_ID) LocalRef<IClientItemExtensions> extensionsRef) {
        if (extensionsRef.get() != IClientItemExtensions.DEFAULT || nytheriacompat$GET_ARMOR_MODEL_HOOK_OVERRIDE.test(this.getClass())) {
            this.nytheriacompat$wrappedHumanoidModel.getWrapped().set(modelRef.get());
            original.call(instance, holder, poseStack, multiBufferSource, i, armorTrim, this.nytheriacompat$wrappedHumanoidModel, bl);
        } else {
            original.call(instance, holder, poseStack, multiBufferSource, i, armorTrim, humanoidModel, bl);
        }
    }

    @WrapOperation(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderGlint(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/HumanoidModel;)V"))
    private void nytheriacompat$tryUseWrapperForGlint(HumanoidArmorLayer<T, M, A> instance, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, A humanoidModel, Operation<Void> original, @Share(value = "model", namespace = NytheriaCompat.MOD_ID) LocalRef<Model> modelRef, @Share(value = "extensions", namespace = NytheriaCompat.MOD_ID) LocalRef<IClientItemExtensions> extensionsRef) {
        if (extensionsRef.get() != IClientItemExtensions.DEFAULT || nytheriacompat$GET_ARMOR_MODEL_HOOK_OVERRIDE.test(this.getClass())) {
            this.nytheriacompat$wrappedHumanoidModel.getWrapped().set(modelRef.get());
            original.call(instance, poseStack, multiBufferSource, i, this.nytheriacompat$wrappedHumanoidModel);
        } else {
            original.call(instance, poseStack, multiBufferSource, i, humanoidModel);
        }
    }

    protected Model getArmorModelHook(T entity, ItemStack stack, EquipmentSlot slot, A model) {
        return ClientHooks.getArmorModel(entity, stack, slot, model);
    }

    public void nytheriacompat$i$renderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, T livingEntity, EquipmentSlot slot, int packedLight, A model, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (this.nytheriacompat$isRunningCompatibility.getAndSet(false)) {
            return;
        }

        this.nytheriacompat$renderLayerData.update(limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        this.renderArmorPiece(poseStack, bufferSource, livingEntity, slot, packedLight, model);
    }

    // Nytheria Compat: let's just. copy these. Mowzie's Mobs needs these.
    protected void renderModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Model model, int dyeColor, ResourceLocation textureLocation) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.armorCutoutNoCull(textureLocation));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, dyeColor);
    }

    protected void renderTrim(Holder<ArmorMaterial> armorMaterial, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ArmorTrim trim, Model model, boolean innerTexture) {
        TextureAtlasSprite textureAtlasSprite = this.armorTrimAtlas.getSprite(innerTexture ? trim.innerTexture(armorMaterial) : trim.outerTexture(armorMaterial));
        VertexConsumer vertexConsumer = textureAtlasSprite.wrap(bufferSource.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal())));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    }

    protected void renderGlint(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Model model) {
        model.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.armorEntityGlint()), packedLight, OverlayTexture.NO_OVERLAY);
    }
}
