/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.block.model;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface BlockElementFaceInjection {
    static BlockElementFace create(@Nullable Direction cullForDirection, int tintIndex, String texture, BlockFaceUV uv, @Nullable ExtraFaceData faceData) {
        return create(cullForDirection, tintIndex, texture, uv, faceData, new MutableObject<>());
    }

    static BlockElementFace create(@Nullable Direction cullForDirection, int tintIndex, String texture, BlockFaceUV uv, @Nullable ExtraFaceData faceData, MutableObject<BlockElement> parent) {
        var face = new BlockElementFace(cullForDirection, tintIndex, texture, uv);
        face.nytheriacompat$setFaceData(faceData);
        face.nytheriacompat$setParent(parent);
        return face;
    }

    default MutableObject<BlockElement> parent() {
        throw NytheriaCompatHelper.createMixinException(BlockElementFaceInjection.class, "parent");
    }

    default void nytheriacompat$setParent(MutableObject<BlockElement> parent) {
        throw NytheriaCompatHelper.createMixinException(BlockElementFaceInjection.class, "nytheriacompat$setParent");
    }

    default ExtraFaceData faceData() {
        throw NytheriaCompatHelper.createMixinException(BlockElementFaceInjection.class, "faceData");
    }

    default void nytheriacompat$setFaceData(ExtraFaceData faceData) {
        throw NytheriaCompatHelper.createMixinException(BlockElementFaceInjection.class, "nytheriacompat$setFaceData");
    }
}
