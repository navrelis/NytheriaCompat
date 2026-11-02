/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block.entity;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import fr.catcore.cursedmixinextensions.annotations.ShadowSuper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.Extends;
import dev.nytheria.compat.util.NytheriaCompatHelper;
import dev.nytheria.compat.workarounds.AttachmentHolderWorkaround;

import java.util.function.Supplier;

@Extends(AttachmentHolder.class)
@Mixin(BlockEntity.class)
public abstract class BlockEntityInject implements IBlockEntityExtension, AttachmentHolderWorkaround, IAttachmentHolder {
    @Shadow public abstract BlockEntityType<?> getType();
    @Shadow public abstract void setChanged();

    @ShadowSuper("setData")
    public abstract <T> T nytheriacompat$super$setData(AttachmentType<T> type, T data);

    @ShadowSuper("removeData")
    public abstract @Nullable <T> T nytheriacompat$super$removeData(AttachmentType<T> type);

    @ModifyReceiver(method = "isValidBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntityType;isValid(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private BlockEntityType<?> nytheriacompat$tryUseCustomGetType(BlockEntityType<?> instance, BlockState state) {
        // Nytheria Compat: vanilla method name, so it has to be resolved to the runtime name, see NytheriaCompatHelper#mapVanillaMethodName
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverrideWithReturnType(this.getClass(), BlockEntity.class, NytheriaCompatHelper.BLOCK_ENTITY_GET_TYPE, BlockEntityType.class)) {
            return this.getType();
        }

        return instance;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void nytheriacompat$handleAdditionalLoad(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (tag.contains(AttachmentHolder.ATTACHMENTS_NBT_KEY, CompoundTag.TAG_COMPOUND)) {
            this.deserializeAttachments(registries, tag.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY));
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void nytheriacompat$handleAdditionalSave(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        var attachments = this.serializeAttachments(registries);

        if (attachments != null) {
            tag.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, attachments);
        }
    }

    @Override
    public CompoundTag getPersistentData() {
        // Nytheria Compat: Redirect to Porting Lib
        return ((BlockEntity) (Object) this).getPortingLibPersistentData();
    }

    @Override
    public <T> T setData(AttachmentType<T> type, T data) {
        this.setChanged();
        return this.nytheriacompat$super$setData(type, data);
    }

    @Override
    public @Nullable <T> T removeData(AttachmentType<T> type) {
        this.setChanged();
        return this.nytheriacompat$super$removeData(type);
    }

    @Override
    public void syncData(AttachmentType<?> type) {
        AttachmentSync.syncBlockEntityUpdate((BlockEntity) (Object) this, type);
    }

    @Mixin(targets = "net.minecraft.world.level.block.entity.BlockEntity$DataComponentInput")
    public interface DataComponentInputInject {
        @Shadow
        @Nullable <T> T get(DataComponentType<T> component);

        @Shadow
        <T> T getOrDefault(DataComponentType<? extends T> component, T defaultValue);

        @Nullable
        default <T> T get(Supplier<? extends DataComponentType<T>> componentType) {
            return this.get(componentType.get());
        }

        default <T> T getOrDefault(Supplier<? extends DataComponentType<T>> componentType, T value) {
            return this.getOrDefault(componentType.get(), value);
        }
    }
}
