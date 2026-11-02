/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@FabricInjectedInterface(ChunkAccess.class)
public interface ChunkAccessInjection {
    default void findBlocks(BiPredicate<BlockState, BlockPos> fineFilter, BiConsumer<BlockPos, BlockState> output) {
        throw NytheriaCompatHelper.createMixinException(ChunkAccessInjection.class, "findBlocks");
    }

    default void findBlocks(Predicate<BlockState> predicate, BiPredicate<BlockState, BlockPos> fineFilter, BiConsumer<BlockPos, BlockState> output) {
        throw NytheriaCompatHelper.createMixinException(ChunkAccessInjection.class, "findBlocks");
    }

    @Nullable
    default CompoundTag writeAttachmentsToNBT(HolderLookup.Provider provider) {
        throw NytheriaCompatHelper.createMixinException(ChunkAccessInjection.class, "writeAttachmentsToNBT");
    }

    default void readAttachmentsFromNBT(HolderLookup.Provider provider, CompoundTag tag) {
        throw NytheriaCompatHelper.createMixinException(ChunkAccessInjection.class, "readAttachmentsFromNBT");
    }

    default AttachmentHolder.AsField getAttachmentHolder() {
        throw NytheriaCompatHelper.createMixinException(ChunkAccessInjection.class, "getAttachmentHolder");
    }

    @Nullable
    default Level getLevel() {
        return null;
    }
}
