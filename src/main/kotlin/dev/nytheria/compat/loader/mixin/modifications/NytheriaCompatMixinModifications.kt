/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.mixin.modifications

import com.llamalad7.mixinextras.sugar.Share
import net.fabricmc.loader.api.FabricLoader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.spongepowered.asm.mixin.injection.*
import dev.nytheria.compat.NytheriaCompat
import dev.nytheria.compat.loader.mixin.modifications.modifiers.*
import dev.nytheria.compat.loader.mixin.modifications.modifiers.AnnotationBasedModifier.NameRemappingAnnotationModifier
import dev.nytheria.compat.loader.remap.NytheriaCompatRemapper
import dev.nytheria.compat.loader.remap.MixinHelpers
import dev.nytheria.compat.loader.remap.MixinTypes

object NytheriaCompatMixinModifications {
    val MIXIN_CLASSES = mutableSetOf<String>()
    private val MODIFIERS = mutableMapOf<String, List<MixinModifier>>()

    val INJECT = register(
        Inject::class.java,

        // Fixes Cataclysm's HumanoidArmorLayerMixin
        InjectedShareAccessModifier(
            owner = "net/minecraft/client/renderer/entity/layers/HumanoidArmorLayer",
            methods = listOf("renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V"),
            paramToShareMapping = mapOf(
                ParamPair("F", 0) to Share("limbSwing", namespace = NytheriaCompat.MOD_ID),
                ParamPair("F", 1) to Share("limbSwingAmount", namespace = NytheriaCompat.MOD_ID),
                ParamPair("F", 2) to Share("partialTick", namespace = NytheriaCompat.MOD_ID),
                ParamPair("F", 3) to Share("ageInTicks", namespace = NytheriaCompat.MOD_ID),
                ParamPair("F", 4) to Share("netHeadYaw", namespace = NytheriaCompat.MOD_ID),
                ParamPair("F", 5) to Share("headPitch", namespace = NytheriaCompat.MOD_ID),
            )
        ),

        // Goes along with the above
        NameRemappingAnnotationModifier(
            owner = "net/minecraft/client/renderer/entity/layers/HumanoidArmorLayer",
            methods = listOf("renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V"),
            remapMethodsTo = listOf(vanillaRenderArmorPiece())
        ),
    )

    private fun vanillaRenderArmorPiece(): String {
        val descriptor = "(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V"
        val name = FabricLoader.getInstance().mappingResolver.mapMethodName(
            "intermediary", "net.minecraft.class_970", "method_4169", NytheriaCompatRemapper.remapDescriptor(descriptor, toIntermediary = true)
        )

        return name + NytheriaCompatRemapper.remapDescriptor(descriptor)
    }

    fun getBaseAnnotation(annotation: AnnotationNode): AnnotationNode {
        var annotation = annotation
        if (annotation.desc == MixinTypes.SUGAR_WRAPPER.descriptor || annotation.desc == MixinTypes.FACTORY_REDIRECT_WRAPPER.descriptor) {
            val map = MixinHelpers.annotationValuesToMap(annotation.values)

            if (map.containsKey("original")) {
                annotation = map["original"] as AnnotationNode
            }
        }

        return annotation
    }

    fun findMatchingModifiers(className: String, annotation: AnnotationNode, descriptor: String): Collection<MixinModifier> {
        val annotation = getBaseAnnotation(annotation)

        val modifiers = MODIFIERS[annotation.desc] ?: emptyList()
        val foundModifiers = mutableListOf<MixinModifier>()
        val map = MixinHelpers.annotationValuesToMap(annotation.values ?: emptyList())

        modifierSearch@for (modifier in modifiers.filter { it.mappedOwner == className || it.owner == className }) {
            when (modifier) {
                is AnnotationBasedModifier -> {
                    if (!modifier.matches(map["method"] ?: continue)) {
                        continue
                    }

                    foundModifiers.add(modifier)
                }

                is InjectedShareAccessModifier -> {
                    if (!modifier.matches(map["method"] ?: continue)) {
                        continue
                    }

                    val descriptor = Type.getArgumentTypes(descriptor)

                    for ((paramPair, _) in modifier.paramToShareMapping) {
                        val (param, ordinal) = paramPair
                        val mappedParam = NytheriaCompatRemapper.remapDescriptor(param)

                        if (descriptor.count { it.descriptor == param || it.descriptor == mappedParam || NytheriaCompatRemapper.remapDescriptor(it.descriptor) == param || NytheriaCompatRemapper.remapDescriptor(it.descriptor) == mappedParam } < ordinal + 1) {
                            continue@modifierSearch
                        }
                    }

                    foundModifiers.add(modifier)
                }

                // must be below everything!
                is MethodBasedModifier -> {
                    if (!modifier.matches(map["method"] ?: continue)) {
                        continue
                    }

                    foundModifiers.add(modifier)
                }
            }
        }

        return foundModifiers
    }

    fun createAnnotation(annotationType: Class<*>, variables: Map<String, Any>): AnnotationNode {
        return createAnnotation(Type.getDescriptor(annotationType), variables)
    }

    fun createAnnotation(annotationType: String, variables: Map<String, Any>): AnnotationNode {
        return AnnotationNode(annotationType).apply {
            this.values = MixinHelpers.mapToAnnotationValues(variables)
        }
    }

    fun finalizeModifier(modifier: MixinModifier) {
        val owner = NytheriaCompatRemapper.remapClass(modifier.owner)
        MIXIN_CLASSES.add(owner)
        modifier.mappedOwner = owner

        if (modifier is MethodBasedModifier) {
            // Nytheria Compat: these are NeoForge's own methods, which keep their names.
            modifier.mappedMethods = modifier.methods.map {
                val name = it.substringBefore('(')
                name + NytheriaCompatRemapper.remapDescriptor(it.removePrefix(name))
            }
        }
    }

    fun register(type: Class<*>, vararg mixinModifiers: MixinModifier): List<MixinModifier> {
        val list = mutableListOf<MixinModifier>()
        val typeDesc = Type.getDescriptor(type)

        for (modifier in mixinModifiers) {
            finalizeModifier(modifier)

            list.add(modifier)
        }

        MODIFIERS[typeDesc] = list
        return list
    }
}
