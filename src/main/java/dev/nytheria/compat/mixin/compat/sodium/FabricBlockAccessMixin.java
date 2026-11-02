/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.compat.sodium;

import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.caffeinemc.mods.sodium.client.util.DirectionUtil;
import net.caffeinemc.mods.sodium.fabric.block.FabricBlockAccess;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

@IfModLoaded("sodium")
@Mixin(FabricBlockAccess.class)
public abstract class FabricBlockAccessMixin {
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$HIDES_NEIGHBOR_FACE_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IBlockExtension.class, "hidesNeighborFace", BlockGetter.class, BlockPos.class, BlockState.class, BlockState.class, Direction.class);
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$SHOULD_DISPLAY_FLUID_OVERLAY_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IBlockExtension.class, "shouldDisplayFluidOverlay", BlockState.class, BlockAndTintGetter.class, BlockPos.class, FluidState.class);
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$SHOULD_HIDE_ADJACENT_FLUID_FACE_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IBlockExtension.class, "shouldHideAdjacentFluidFace", BlockState.class, Direction.class, FluidState.class);

    // Nytheria Compat: NeoForge's answer is otherState.hidesNeighborFace(...) && selfState.supportsExternalFaceHiding().
    //  IBlockExtension#hidesNeighborFace defaults to false, so unless the neighbour's block overrides it the answer is
    //  false whatever supportsExternalFaceHiding says, and false is Sodium's Fabric answer. So only an override of
    //  hidesNeighborFace by the neighbour (otherState) takes over; an override of supportsExternalFaceHiding alone
    //  can't change the result. (The supportsExternalFaceHiding check used to be OR-ed in and was true for every block.)
    @Inject(method = "shouldSkipRender", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$tryUseNeoSkipRender(BlockGetter level, BlockState selfState, BlockState otherState, BlockPos selfPos, BlockPos otherPos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (nytheriacompat$HIDES_NEIGHBOR_FACE_OVERRIDE.test(otherState.getBlock().getClass())) {
            cir.setReturnValue(otherState.hidesNeighborFace(level, otherPos, selfState, DirectionUtil.getOpposite(facing)) && selfState.supportsExternalFaceHiding());
        }
    }

    // Nytheria Compat: IBlockExtension#shouldDisplayFluidOverlay defaults to HalfTransparentBlock || LeavesBlock, which is also
    //  the default of Fabric's FluidRenderHandlerRegistry#isBlockTransparent that Sodium asks (and that one also knows
    //  the blocks Fabric mods registered).
    @Inject(method = "shouldShowFluidOverlay", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$tryUseNeoFluidOverlayCheck(BlockState state, BlockAndTintGetter level, BlockPos pos, FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        if (nytheriacompat$SHOULD_DISPLAY_FLUID_OVERLAY_OVERRIDE.test(state.getBlock().getClass())) {
            cir.setReturnValue(state.shouldDisplayFluidOverlay(level, pos, fluidState));
        }
    }

    @Redirect(method = "getNormalVectorShade", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/fabric/block/FabricBlockAccess;normalShade(Lnet/minecraft/world/level/BlockAndTintGetter;FFFZ)F"))
    private float nytheriacompat$tryUseNeoNormalShade(FabricBlockAccess instance, BlockAndTintGetter blockView, float normalX, float normalY, float normalZ, boolean hasShade) {
        return blockView.getShade(normalX, normalY, normalZ, hasShade);
    }

    // Nytheria Compat: IBlockExtension#shouldHideAdjacentFluidFace defaults to the same fluid type comparison that Sodium's
    //  Fabric shouldOccludeFluid does.
    @Inject(method = "shouldOccludeFluid", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$tryCheckNeoAdjacentFluid(Direction adjDirection, BlockState adjBlockState, FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        if (nytheriacompat$SHOULD_HIDE_ADJACENT_FLUID_FACE_OVERRIDE.test(adjBlockState.getBlock().getClass())) {
            cir.setReturnValue(adjBlockState.shouldHideAdjacentFluidFace(adjDirection, fluid));
        }
    }
}
