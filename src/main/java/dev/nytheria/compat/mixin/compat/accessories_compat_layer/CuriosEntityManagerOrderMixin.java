package dev.nytheria.compat.mixin.compat.accessories_compat_layer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Nytheria Compat: fix for another mod's reload order, seen by Nytheria Compat users because Curios (and the mods that use it, like
 * L_Ender's Cataclysm) run through Nytheria Compat on top of the Accessories Compatibility Layer ("cclayer").
 * <p>
 * cclayer replaces Curios' slot storage with Accessories'. Its reload listener
 * {@code accessories_compat_layer:curios_entity_manager} (Curios' {@code CuriosEntityManager}) looks every slot it
 * binds to an entity up in Accessories' {@code SlotTypeLoader}, but it is only made a dependency of
 * {@code accessories:entity_slot_loader}. Nothing tells Fabric API's reload listener sort that it has to run after
 * {@code accessories:slot_loader}, which is where the converted Curios slots get registered. Depending on the sort it
 * runs before it, and Curios logs "rings is not a registered slot type!" (also head, necklace, belt, hands, feet, waist,
 * talisman, informational) and drops those entity bindings. Until the first {@code /reload} the player then has no
 * {@code rings}, {@code waist}, {@code talisman} or {@code informational} slot, so Cataclysm's belts, Ring of Grudged
 * and Unbreakable Skull can't be equipped.
 * <p>
 * cclayer wraps the (not Fabric aware) Curios listener in its {@code ModifiableIdentifiableResourceReloadListener}
 * record; this adds the missing {@code accessories:slot_loader} dependency to that one listener. That can't form a
 * cycle: {@code slot_loader} only depends on cclayer's {@code curios_slot_manager}, and
 * {@code entity_slot_loader} (which depends on {@code curios_entity_manager}) already comes after {@code slot_loader}.
 * The mixin is a no-op when cclayer is absent or renames the class, and if cclayer starts declaring the dependency
 * itself the {@code add} changes nothing.
 */
@Pseudo
@IfModLoaded("accessories_compat_layer")
@Mixin(targets = "io.wispforest.accessories_compat.utils.ModifiableIdentifiableResourceReloadListener", remap = false)
public abstract class CuriosEntityManagerOrderMixin {
    private static final ResourceLocation NYTHERIACOMPAT$CURIOS_ENTITY_MANAGER =
        ResourceLocation.fromNamespaceAndPath("accessories_compat_layer", "curios_entity_manager");
    private static final ResourceLocation NYTHERIACOMPAT$ACCESSORIES_SLOT_LOADER =
        ResourceLocation.fromNamespaceAndPath("accessories", "slot_loader");

    @Shadow(remap = false)
    public abstract ResourceLocation getFabricId();

    @ModifyReturnValue(method = "getFabricDependencies", at = @At("RETURN"), require = 0, remap = false)
    private Collection<ResourceLocation> nytheriacompat$loadAfterAccessoriesSlotLoader(Collection<ResourceLocation> dependencies) {
        if (!NYTHERIACOMPAT$CURIOS_ENTITY_MANAGER.equals(this.getFabricId()) || dependencies.contains(NYTHERIACOMPAT$ACCESSORIES_SLOT_LOADER)) {
            return dependencies;
        }
        Set<ResourceLocation> merged = new HashSet<>(dependencies);
        merged.add(NYTHERIACOMPAT$ACCESSORIES_SLOT_LOADER);
        return merged;
    }
}
