// TRACKED HASH: 530ffd564fc9a3e6a9785a5c90873c529567a4cc
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.math;

import com.mojang.math.Transformation;
import net.neoforged.neoforge.common.extensions.ITransformationExtension;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Transformation.class)
public class TransformationInject implements ITransformationExtension {
    @Shadow @Final private Matrix4f matrix;
    @Unique private Matrix3f normalTransform = null;

    public Matrix3f getNormalMatrix() {
        checkNormalTransform();
        return normalTransform;
    }

    private void checkNormalTransform() {
        if (normalTransform == null) {
            normalTransform = new Matrix3f(matrix);
            normalTransform.invert();
            normalTransform.transpose();
        }
    }
}