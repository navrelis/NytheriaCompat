// TRACKED HASH: 834fc3d6b652f61039af4bb7ec29cdea5fe3bc0e
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.tags;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.tags.BlockTagsInjection;

@Mixin(BlockTags.class)
public class BlockTagsInject implements BlockTagsInjection {
    @CreateStatic
    private static TagKey<Block> create(ResourceLocation name) {
        return BlockTagsInjection.create(name);
    }
}