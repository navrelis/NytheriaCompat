// TRACKED HASH: a1f8c952c92e35e0c9d786ea3cad6f768d53a153
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import com.google.common.collect.Maps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.world.level.block.FlowerPotBlockInjection;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(FlowerPotBlock.class)
public abstract class FlowerPotBlockInject extends Block implements FlowerPotBlockInjection {
    @Shadow @Final @Mutable private Block potted;
    @Shadow public abstract Block getPotted();

    @Unique private Map<ResourceLocation, Supplier<? extends Block>> fullPots = Maps.newHashMap();
    @Unique private Supplier<FlowerPotBlock> emptyPot;
    @Unique private Supplier<? extends Block> flowerDelegate;

    public FlowerPotBlockInject(Properties properties) {
        super(properties);
    }

    @CreateInitializer
    public FlowerPotBlockInject(@Nullable Supplier<FlowerPotBlock> emptyPot, Supplier<? extends Block> block, BlockBehaviour.Properties properties) {
        super(properties);
        this.potted = null;
        this.flowerDelegate = block;
        if (emptyPot == null) {
            this.fullPots = Maps.newHashMap();
            this.emptyPot = null;
        } else {
            this.fullPots = Collections.emptyMap();
            this.emptyPot = emptyPot;
        }
    }

    @Unique
    private static final String NYTHERIACOMPAT$GET_POTTED = FabricLoader.getInstance().getMappingResolver().mapMethodName(
        "intermediary", "net.minecraft.class_2362", "method_16231", "()Lnet/minecraft/class_2248;"
    );

    public FlowerPotBlock getEmptyPot() {
        return emptyPot == null ? (FlowerPotBlock) (Object) this : emptyPot.get();
    }

    public Map<ResourceLocation, Supplier<? extends Block>> getFullPotsView() {
        return Collections.unmodifiableMap(fullPots);
    }
}
