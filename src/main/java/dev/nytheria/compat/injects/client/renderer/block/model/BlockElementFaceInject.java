// TRACKED HASH: c651e93bacb2243f18c5b16ca365e199444ccfb3
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.block.model;

import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.client.renderer.block.model.BlockElementFaceInjection;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.core.Direction;

@Mixin(BlockElementFace.class)
public abstract class BlockElementFaceInject implements BlockElementFaceInjection {
    @Unique private ExtraFaceData faceData;
    @Unique private MutableObject<BlockElement> parent = new MutableObject<>();

    public BlockElementFaceInject(@Nullable Direction cullForDirection, int tintIndex, String texture, BlockFaceUV uv) {}

    @CreateInitializer
    public BlockElementFaceInject(@Nullable Direction cullForDirection, int tintIndex, String texture, BlockFaceUV uv, @Nullable ExtraFaceData faceData, MutableObject<BlockElement> parent) {
        this(cullForDirection, tintIndex, texture, uv);
        this.faceData = faceData;
        this.parent = parent;
    }

    @Override
    public MutableObject<BlockElement> parent() {
        return this.parent;
    }

    @Override
    public ExtraFaceData faceData() {
        if (this.faceData != null)
            return this.faceData;
        else if (this.parent.getValue() != null)
            return this.parent.getValue().getFaceData();

        return ExtraFaceData.DEFAULT;
    }

    @Override
    public void nytheriacompat$setFaceData(ExtraFaceData faceData) {
        this.faceData = faceData;
    }

}
