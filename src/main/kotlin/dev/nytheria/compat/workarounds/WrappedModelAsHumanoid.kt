/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.Model
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.LivingEntity
import java.util.concurrent.atomic.AtomicReference

class WrappedModelAsHumanoid(val wrapped: AtomicReference<Model> = AtomicReference()) : HumanoidModel<LivingEntity>(ModelPart(listOf(), mapOf(
    "head" to ModelPart(listOf(), mapOf()),
    "hat" to ModelPart(listOf(), mapOf()),
    "body" to ModelPart(listOf(), mapOf()),
    "right_arm" to ModelPart(listOf(), mapOf()),
    "left_arm" to ModelPart(listOf(), mapOf()),
    "right_leg" to ModelPart(listOf(), mapOf()),
    "left_leg" to ModelPart(listOf(), mapOf()),
)), { wrapped.get().renderType(it) }) {
    override fun setupAnim(entity: LivingEntity, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float) {
    }

    override fun renderToBuffer(poseStack: PoseStack, buffer: VertexConsumer, packedLight: Int, packedOverlay: Int, color: Int) {
        this.wrapped.get().renderToBuffer(poseStack, buffer, packedLight, color)
    }
}