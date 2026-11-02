// TRACKED HASH: c1c579e966fc78d57072df71b93acdf023e75c9e
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.vehicle;

import java.util.function.Supplier;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.fml.common.asm.enumextension.*;
import net.neoforged.neoforge.common.extensions.IBoatExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.entity.vehicle.BoatInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

@Mixin(Boat.class)
public abstract class BoatInject extends Entity implements IBoatExtension {
    @Shadow private int lerpSteps;
    @Shadow private double lerpX;
    @Shadow private double lerpY;
    @Shadow private double lerpZ;
    @Shadow private double lerpYRot;
    @Shadow private double lerpXRot;
    @Shadow public abstract Boat.Type getVariant();

    public BoatInject(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @WrapOperation(method = {"getWaterLevelAbove", "checkInWater", "isUnderwater"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    public boolean nytheriacompat$checkIfBoatingInFluid(FluidState instance, TagKey<Fluid> tag, Operation<Boolean> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(this.getClass(), Boat.class, "canBoatInFluid", FluidState.class)) {
            return this.canBoatInFluid(instance);
        }

        return original.call(instance, tag);
    }

    @WrapOperation(method = "checkFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    public boolean nytheriacompat$checkIfBoatIsInFluidBeforeState(FluidState instance, TagKey<Fluid> tag, Operation<Boolean> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(this.getClass(), Boat.class, "canBoatInFluid", FluidState.class)) {
            return !this.canBoatInFluid(instance);
        }

        return original.call(instance, tag);
    }

    @WrapOperation(method = "canAddPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/Boat;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    public boolean nytheriacompat$checkIfCanBoatBeforePassenger(Boat instance, TagKey tagKey, Operation<Boolean> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(this.getClass(), Boat.class, "canBoatInFluid", FluidState.class)) {
            return this.canBoatInFluid(this.getEyeInFluidType());
        }

        return original.call(instance, tagKey);
    }

    // a forge fix, might as well
    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (this.isControlledByLocalInstance() && this.lerpSteps > 0) {
            this.lerpSteps = 0;
            this.absMoveTo(this.lerpX, this.lerpY, this.lerpZ, (float) this.lerpYRot, (float) this.lerpXRot);
        }
    }

    @NamedEnum(1)
    @NetworkedEnum(NetworkedEnum.NetworkCheck.CLIENTBOUND)
    @Mixin(Boat.Type.class)
    public abstract static class TypeInject implements BoatInjection.TypeInjection, IExtensibleEnum {
        @Shadow @Final private Block planks;

        @Unique private Supplier<Block> planksSupplier;
        @Unique Supplier<Item> boatItem = () -> Items.AIR;
        @Unique Supplier<Item> chestBoatItem = () -> Items.AIR;
        @Unique private Supplier<Item> stickItem = () -> Items.STICK;
        @Unique private boolean raft;

        @ReservedConstructor
        private TypeInject(String fieldName, int ordinal, Block planks, String name) {}

        @CreateInitializer
        @ReservedConstructor
        private TypeInject(String fieldName, int ordinal, Block planks, String name, boolean raft) {
            this(fieldName, ordinal, planks, name);
            this.raft = raft;
        }

        @CreateInitializer
        private TypeInject(String fieldName, int ordinal, Supplier<Block> planks, String name, Supplier<Item> boatItem, Supplier<Item> chestBoatItem, Supplier<Item> stickItem, boolean raft) {
            this(fieldName, ordinal, Blocks.AIR, name);
            this.planksSupplier = planks;
            this.boatItem = boatItem;
            this.chestBoatItem = chestBoatItem;
            this.stickItem = stickItem;
            this.raft = raft;
        }

        @Override
        public Item getSticks() {
            return this.stickItem.get();
        }

        @Override
        public boolean isRaft() {
            return this.raft;
        }

        @Override
        public void nytheriacompat$setRaft(boolean raft) {
            this.raft = raft;
        }

        @Override
        public Supplier<Item> nytheriacompat$getBoatItem() {
            return this.boatItem;
        }

        @Override
        public Supplier<Item> nytheriacompat$getChestBoatItem() {
            return this.chestBoatItem;
        }

        @CreateStatic
        private static ExtensionInfo getExtensionInfo() {
            return ExtensionInfo.nonExtended(Boat.Type.class);
        }
    }
}
