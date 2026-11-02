// TRACKED HASH: 84f534b4bbc58f38de24da827b479b0d0083faaf
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.block.model;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.client.renderer.block.model.BlockElementInjection;

import java.util.Map;
import java.util.Objects;

@Mixin(BlockElement.class)
public abstract class BlockElementInject implements BlockElementInjection {
    @Shadow @Final public Map<Direction, BlockElementFace> faces;
    @Unique private ExtraFaceData faceData = ExtraFaceData.DEFAULT;

    public BlockElementInject(Vector3f from, Vector3f to, Map<Direction, BlockElementFace> faces, @Nullable BlockElementRotation rotation, boolean shade) {}

    @CreateInitializer
    public BlockElementInject(Vector3f from, Vector3f to, Map<Direction, BlockElementFace> faces, @Nullable BlockElementRotation rotation, boolean shade, ExtraFaceData faceData) {
        this(from, to, faces, rotation, shade);
        this.setFaceData(faceData);
        this.nytheriacompat$setFaces();
    }

    @Override
    public void nytheriacompat$setFaces() {
        this.faces.values().forEach(face -> face.parent().setValue((BlockElement) (Object) this));
    }

    @Override
    public ExtraFaceData getFaceData() {
        return faceData;
    }

    @Override
    public void setFaceData(ExtraFaceData faceData) {
        this.faceData = Objects.requireNonNull(faceData);
    }

}