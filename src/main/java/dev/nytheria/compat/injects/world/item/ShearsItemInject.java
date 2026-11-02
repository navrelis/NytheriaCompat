/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(ShearsItem.class)
public abstract class ShearsItemInject extends Item {
    public ShearsItemInject(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(itemAbility);
    }

//    TODO: nytheria_compat implement patch
//    @Override
//    public InteractionResult useOn(UseOnContext p_186371_) {
//        Level level = p_186371_.getLevel();
//        BlockPos blockpos = p_186371_.getClickedPos();
//        BlockState blockstate = level.getBlockState(blockpos);
//        -        if (blockstate.getBlock() instanceof GrowingPlantHeadBlock growingplantheadblock && !growingplantheadblock.isMaxAge(blockstate)) {
//            +        BlockState blockstate1 = blockstate.getToolModifiedState(p_186371_, net.neoforged.neoforge.common.ItemAbilities.SHEARS_TRIM, false);
//            +        if (blockstate1 != null) {
//                Player player = p_186371_.getPlayer();
//                ItemStack itemstack = p_186371_.getItemInHand();
//                if (player instanceof ServerPlayer) {
//                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
//                }
//
//                -            level.playSound(player, blockpos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
//                -            BlockState blockstate1 = growingplantheadblock.getMaxAgeState(blockstate);
//                level.setBlockAndUpdate(blockpos, blockstate1);
//                level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(p_186371_.getPlayer(), blockstate1));
//                if (player != null) {
}
