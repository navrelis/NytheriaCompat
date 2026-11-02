/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.block.model;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.Map;

public interface BlockElementInjection {
    static BlockElement create(Vector3f from, Vector3f to, Map<Direction, BlockElementFace> faces, @Nullable BlockElementRotation rotation, boolean shade, ExtraFaceData faceData) {
        var element = new BlockElement(from, to, faces, rotation, shade);
        element.setFaceData(faceData);
        element.nytheriacompat$setFaces();
        return element;
    }

    default ExtraFaceData getFaceData() {
        throw NytheriaCompatHelper.createMixinException(BlockElementInjection.class, "getFaceData");
    }

    default void setFaceData(ExtraFaceData faceData) {
        throw NytheriaCompatHelper.createMixinException(BlockElementInjection.class, "setFaceData");
    }

    default void nytheriacompat$setFaces() {
        throw NytheriaCompatHelper.createMixinException(BlockElementInjection.class, "nytheriacompat$setFaces");
    }
}
