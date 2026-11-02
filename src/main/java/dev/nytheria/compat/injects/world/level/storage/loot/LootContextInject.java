/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.storage.loot;

import io.github.fabricators_of_create.porting_lib.loot.extensions.LootContextExtensions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.mixin.world.level.storage.loot.LootContextAccessor;

import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;

@Mixin(LootContext.class)
public abstract class LootContextInject implements LootContextExtensions {
    private LootContextInject(LootParams params, RandomSource random, HolderGetter.Provider provider) {}

    @CreateInitializer
    private LootContextInject(LootParams params, RandomSource random, HolderGetter.Provider provider, ResourceLocation queriedLootTableId) {
        this(params, random, provider);
        this.setQueriedLootTableId(queriedLootTableId);
    }

    @Mixin(LootContext.Builder.class)
    public abstract static class BuilderInject {
        @Shadow @Nullable private RandomSource random;


        public BuilderInject(LootParams params) {}

        @CreateInitializer
        public BuilderInject(LootContext context) {
            this(((LootContextAccessor) context).getParams());
            this.random = ((LootContextAccessor) context).getRandom();
        }
    }
}
