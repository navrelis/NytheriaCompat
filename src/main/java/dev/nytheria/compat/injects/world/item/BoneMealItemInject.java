/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateStatic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemInject extends Item {
    @Shadow
    public static boolean growCrop(ItemStack stack, Level level, BlockPos pos) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Unique private static final ThreadLocal<Player> nytheriacompat$playerRef = new ThreadLocal<>();

    public BoneMealItemInject(Properties properties) {
        super(properties);
    }

    @CreateStatic
    private static boolean applyBonemeal(ItemStack stack, Level level, BlockPos pos, @Nullable Player player) {
        nytheriacompat$playerRef.set(player);
        boolean value = growCrop(stack, level, pos);
        nytheriacompat$playerRef.remove();
        return value;
    }
}
