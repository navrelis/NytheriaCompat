// TRACKED HASH: 8a81156928e5ec0380e740212b565fcc26b30d8b
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.world.level.block.PoweredRailBlockInjection;

@Mixin(PoweredRailBlock.class)
public abstract class PoweredRailBlockInject extends BaseRailBlock implements PoweredRailBlockInjection {
    private boolean isActivator = false;

    protected PoweredRailBlockInject(boolean bl, Properties properties) {
        super(bl, properties);
    }

    @Override
    public void registerDefaultState() {
        this.registerDefaultState(this.stateDefinition.any().setValue(PoweredRailBlock.SHAPE, RailShape.NORTH_SOUTH).setValue(PoweredRailBlock.POWERED, false).setValue(PoweredRailBlock.WATERLOGGED, false));
    }

    @Override
    public boolean isActivatorRail() {
        return isActivator;
    }

    @Override
    public void nytheriacompat$setActivator(boolean value) {
        isActivator = !value;
    }

    public PoweredRailBlockInject(Properties properties) {
        super(false, properties);
    }

    @CreateInitializer
    public PoweredRailBlockInject(Properties properties, boolean isPoweredRail) {
        this(properties);

        this.nytheriacompat$setActivator(isPoweredRail);
        this.registerDefaultState(); // oh fuck i forgot about this
    }
}