// TRACKED HASH: 979c34bac4c2b61ba34abcb42332036ed82e63ee
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

@Mixin(ChestRenderer.class)
public abstract class ChestRendererInject<T extends BlockEntity> implements BlockEntityRenderer<T> {
    @Shadow private boolean xmasTextures;

    @Unique private boolean nytheriacompat$isDefault = false;

    protected Material getMaterial(T blockEntity, ChestType chestType) {
        nytheriacompat$isDefault = true;
        return Sheets.chooseMaterial(blockEntity, chestType, this.xmasTextures);
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return AABB.encapsulatingFullBlocks(pos.offset(-1, 0, -1), pos.offset(1, 1, 1));
    }
}