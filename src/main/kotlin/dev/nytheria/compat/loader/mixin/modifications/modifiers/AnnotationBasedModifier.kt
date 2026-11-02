/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.mixin.modifications.modifiers

import org.objectweb.asm.tree.AnnotationNode
import org.spongepowered.asm.mixin.transformer.ClassInfo
import dev.nytheria.compat.loader.mixin.modifications.NytheriaCompatMixinModifications
import dev.nytheria.compat.loader.remap.MixinHelpers
import dev.nytheria.compat.loader.remap.MixinTypes

sealed interface AnnotationBasedModifier : MethodBasedModifier {
    fun modifyMixin(classInfo: ClassInfo, annotation: AnnotationNode, newAnnotations: MutableList<AnnotationNode>)

    data class NameRemappingAnnotationModifier(
        override val owner: String,
        override val methods: List<String> = listOf(),

        var remapMethodsTo: List<String>,
    ) : AnnotationBasedModifier {
        override lateinit var mappedOwner: String
        override lateinit var mappedMethods: List<String>

        override fun modifyMixin(classInfo: ClassInfo, annotation: AnnotationNode, newAnnotations: MutableList<AnnotationNode>) {
            val newAnnotation = run {
                val annotation = NytheriaCompatMixinModifications.getBaseAnnotation(annotation)

                NytheriaCompatMixinModifications.createAnnotation(annotation.desc,
                    MixinHelpers.annotationValuesToMap(annotation.values).toMutableMap().apply {
                        this["method"] = remapMethodsTo
                    })
            }

            if (annotation.desc == MixinTypes.SUGAR_WRAPPER.descriptor || annotation.desc == MixinTypes.FACTORY_REDIRECT_WRAPPER.descriptor) {
                val map = MixinHelpers.annotationValuesToMap(annotation.values).toMutableMap()
                map["original"] = newAnnotation
                annotation.values = MixinHelpers.mapToAnnotationValues(map)

                newAnnotations.add(annotation)
            } else {
                newAnnotations.add(newAnnotation)
            }
        }
    }
}
