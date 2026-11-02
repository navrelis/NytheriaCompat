// TRACKED HASH: d24928420f3c1ebf622411bc07206c361aa737b9
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.server.ReloadableServerResourcesInjection;
import dev.nytheria.compat.injections.world.item.crafting.IngredientInjection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesInject implements ReloadableServerResourcesInjection {
    @Shadow
    @Final
    private ReloadableServerResources.ConfigurableRegistryLookup registryLookup;
    @Shadow
    @Final
    private ReloadableServerRegistries.Holder fullRegistryHolder;

    @NotNull
    @Override
    public ICondition.IContext getConditionContext() {
        return ICondition.IContext.EMPTY;
    }

    @Override
    public HolderLookup.Provider getRegistryLookup() {
        return registryLookup;
    }

    @ModifyArg(method = "method_58296", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/SimpleReloadInstance;create(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Z)Lnet/minecraft/server/packs/resources/ReloadInstance;"))
    private static List<PreparableReloadListener> nytheriacompat$addForgeResourceReloadListener(List<PreparableReloadListener> listeners, @Local ReloadableServerResources serverResources, @Local(argsOnly = true) LayeredRegistryAccess<RegistryLayer> registryAccess, @Local(argsOnly = true) Commands.CommandSelection commandSelection) {
        var list = new ArrayList<>(listeners);
        list.addAll(EventHooks.onResourceReload(serverResources, registryAccess.compositeAccess()));
        list.forEach(rl -> {
            if (rl instanceof ContextAwareReloadListener srl) srl.injectContext(serverResources.getConditionContext(), serverResources.getRegistryLookup());
        });
        return list;
    }

    // Nytheria Compat: the tags are bound now. Ingredients that were asked for their items before that (while the recipes were
    //  loaded) resolve them again; this runs before anything else can look at them with the bound tags.
    @Inject(method = "updateRegistryTags()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;invalidateCache()V"))
    private void nytheriacompat$retryItemsResolvedBeforeTags(CallbackInfo ci) {
        IngredientInjection.EMPTY_BEFORE_TAGS.tagsBound();
    }

    @Inject(method = "updateRegistryTags()V", at = @At("TAIL"))
    private void nytheriacompat$callTagUpdateEvent(CallbackInfo ci) {
        if (EventGate.has(TagsUpdatedEvent.class)) NeoForge.EVENT_BUS.post(new TagsUpdatedEvent(this.fullRegistryHolder.get(), false, false));
    }
}
