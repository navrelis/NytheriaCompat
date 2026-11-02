/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.raid;

import java.util.function.Supplier;

import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.ReservedConstructor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.entity.raid.RaidRaiderTypeInjection;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;

@Mixin(Raid.class)
public abstract class RaidInject {
    @Mixin(Raid.RaiderType.class)
    public abstract static class RaiderTypeInject implements RaidRaiderTypeInjection, IExtensibleEnum {
        @Unique Supplier<EntityType<? extends Raider>> entityTypeSupplier;
        @Unique boolean nytheriacompat$isNeo;

        @ReservedConstructor
        private RaiderTypeInject(String name, int ordinal, EntityType<? extends Raider> entityType, int[] spawnsPerWave) {
        }

        @CreateInitializer
        private RaiderTypeInject(String name, int ordinal, Supplier<EntityType<? extends Raider>> entityTypeSupplier, int[] spawnsPerWave) {
            this(name, ordinal, (EntityType<? extends Raider>) null, spawnsPerWave);
            this.entityTypeSupplier = entityTypeSupplier;
        }

        @CreateStatic
        private static ExtensionInfo getExtensionInfo() {
            return ExtensionInfo.nonExtended(Raid.RaiderType.class);
        }

        @Override
        public boolean nytheriacompat$isNeo() {
            return this.nytheriacompat$isNeo;
        }
    }
}
