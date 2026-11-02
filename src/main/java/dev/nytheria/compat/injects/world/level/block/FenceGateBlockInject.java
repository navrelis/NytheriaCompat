// TRACKED HASH: 13883d3a815e6b78fb2ecf76ce46710d2684597f
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;

@Mixin(FenceGateBlock.class)
public abstract class FenceGateBlockInject extends HorizontalDirectionalBlock {
    public FenceGateBlockInject(WoodType type, Properties properties) {
        super(properties);
    }

    @Unique public SoundEvent openSound, closeSound;
    @Unique private boolean nytheriacompat$isCustomSounds;

    @CreateInitializer
    public FenceGateBlockInject(BlockBehaviour.Properties properties, SoundEvent openSound, SoundEvent closeSound) {
        this(Optional.empty(), properties, Optional.of(openSound), Optional.of(closeSound));
    }

    @CreateInitializer
    public FenceGateBlockInject(Optional<WoodType> woodType, BlockBehaviour.Properties properties, Optional<SoundEvent> openSound, Optional<SoundEvent> closeSound) {
        this(woodType.orElse(
            WoodType.OAK // Nytheria Compat: Use oak as a default
        ), properties);
        this.openSound = openSound.orElseThrow();
        this.closeSound = closeSound.orElseThrow();
        this.nytheriacompat$isCustomSounds = true;
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FenceGateBlock;registerDefaultState(Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void nytheriacompat$useCustomSounds(WoodType type, Properties properties, CallbackInfo ci) {
        if (type != null) {
            this.openSound = type.fenceGateOpen();
            this.closeSound = type.fenceGateClose();
        }
    }
}
