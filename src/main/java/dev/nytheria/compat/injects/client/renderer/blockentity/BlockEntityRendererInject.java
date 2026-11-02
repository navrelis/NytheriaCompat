package dev.nytheria.compat.injects.client.renderer.blockentity;

import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

// Nytheria Compat: NeoForge's getRenderBoundingBox(BlockEntity) is a default method of IBlockEntityRendererExtension, which the
//  class tweaker injects into BlockEntityRenderer. Fabric mods may add a default method with the very same signature
//  through another interface (Entity Culling's BlockEntityRenderFabricExtension), and a renderer that overrides neither
//  (BedRenderer) then has two conflicting defaults: AbstractMethodError on the first call. Declaring the default on
//  BlockEntityRenderer itself makes it the most specific one; it does the same as both of them.
@Mixin(BlockEntityRenderer.class)
public interface BlockEntityRendererInject<T extends BlockEntity> extends IBlockEntityRendererExtension<T> {
    @Override
    default AABB getRenderBoundingBox(T blockEntity) {
        return new AABB(blockEntity.getBlockPos());
    }
}
