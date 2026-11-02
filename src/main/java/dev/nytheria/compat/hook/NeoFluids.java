package dev.nytheria.compat.hook;

import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * The scope test of a fluid hook: does this fluid have a fluid type of a NeoForge mod? Fluid physics go by the fluid and
 * never by the entity: a vanilla mob in such a fluid is pushed as on NeoForge, every entity in water, lava or a Fabric
 * mod's fluid takes the vanilla path. Water and lava have types in the {@code minecraft} namespace, and the wrapper type
 * of a Fabric fluid is not registered, so neither is in scope.
 */
public final class NeoFluids {
    private NeoFluids() {
    }

    /** Built on first use, an entity's first tick, when the mods have registered their fluid types. */
    private static final class Types {
        private static final Set<FluidType> NEO = collect();

        private static Set<FluidType> collect() {
            Set<FluidType> types = new HashSet<>();
            NeoForgeRegistries.FLUID_TYPES.entrySet().forEach(entry -> {
                if (Scope.isNeoNamespace(entry.getKey().location().getNamespace())) types.add(entry.getValue());
            });
            return Set.copyOf(types);
        }
    }

    /** Whether a NeoForge mod has a fluid at all. If not, a hook can skip its pass over the blocks around an entity. */
    public static boolean active() {
        return !Types.NEO.isEmpty();
    }

    public static boolean isNeo(FluidState state) {
        return !state.isEmpty() && isNeoType(state.neo$getFluidType());
    }

    public static boolean isNeoType(FluidType type) {
        return Types.NEO.contains(type);
    }
}
