/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.block.model;

import io.github.fabricators_of_create.porting_lib.models.extensions.ItemTransformExtensions;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.joml.Vector3f;

public interface ItemTransformInjection extends ItemTransformExtensions {
    static ItemTransform create(Vector3f vector3f, Vector3f vector3f2, Vector3f vector3f3, Vector3f rightRotation) {
        var itemTransform = new ItemTransform(vector3f, vector3f2, vector3f3);
        //noinspection RedundantCast
        ((ItemTransformExtensions) itemTransform).setRightRotation(rightRotation);

        return itemTransform;
    }
}
