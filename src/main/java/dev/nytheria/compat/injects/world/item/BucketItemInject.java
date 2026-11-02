/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import java.util.Optional;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.neoforge.common.extensions.IBucketPickupExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.injections.world.item.BucketItemInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(BucketItem.class)
public abstract class BucketItemInject extends Item implements BucketItemInjection {
    // Nytheria Compat: canBlockContainFluid is protected, so ofDeclared: Class.getMethod (what hasMethodOverride uses) only finds public methods
    //  and would never see an override. Both injectors below run on every bucket use, so resolve the check only once.
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$CAN_BLOCK_CONTAIN_FLUID_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.ofDeclared(BucketItem.class, "canBlockContainFluid", Player.class, Level.class, BlockPos.class, BlockState.class);

    @Shadow public Fluid content;
    @Shadow public abstract boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult result);

    public BucketItemInject(Properties properties) {
        super(properties);
    }

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;getPickupSound()Ljava/util/Optional;"))
    private Optional<SoundEvent> nytheriacompat$tryUseStateAwarePickupSound(BucketPickup instance, Operation<Optional<SoundEvent>> original, @Local BlockState state) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(instance.getClass(), IBucketPickupExtension.class, "getPickupSound", BlockState.class)) {
            return instance.getPickupSound(state);
        }

        return original.call(instance);
    }

    @Definition(id = "blockState", local = @Local(type = BlockState.class))
    @Definition(id = "getBlock", method = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;")
    @Definition(id = "LiquidBlockContainer", type = LiquidBlockContainer.class)
    @Expression("blockState.getBlock() instanceof LiquidBlockContainer")
    @ModifyExpressionValue(method = "use", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$tryCheckContainsFluid(boolean original, @Local(argsOnly = true) Player player, @Local(argsOnly = true) Level level, @Local(ordinal = 0) BlockPos pos, @Local BlockState state) {
        if (nytheriacompat$CAN_BLOCK_CONTAIN_FLUID_OVERRIDE.test(this.getClass())) {
            return this.canBlockContainFluid(player, level, pos, state);
        }

        return original;
    }

    @Definition(id = "content", field = "Lnet/minecraft/world/item/BucketItem;content:Lnet/minecraft/world/level/material/Fluid;")
    @Definition(id = "WATER", field = "Lnet/minecraft/world/level/material/Fluids;WATER:Lnet/minecraft/world/level/material/FlowingFluid;")
    @Expression("this.content == WATER")
    @ModifyExpressionValue(method = "use", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$tryUseAlternativeFluidCheck(boolean original, @Local(argsOnly = true) Player player, @Local(argsOnly = true) Level level, @Local(ordinal = 0) BlockPos pos, @Local BlockState state) {
        if (nytheriacompat$CAN_BLOCK_CONTAIN_FLUID_OVERRIDE.test(this.getClass())) {
            return true;
        }

        return original || ((LiquidBlockContainer) state.getBlock()).canPlaceLiquid(player, level, pos, state, this.content);
    }

    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult result, @Nullable ItemStack container) {
        return this.emptyContents(player, level, pos, result);
    }

    protected boolean canBlockContainFluid(@Nullable Player player, Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof LiquidBlockContainer && ((LiquidBlockContainer) state.getBlock()).canPlaceLiquid(player, level, pos, state, this.content);
    }
}
