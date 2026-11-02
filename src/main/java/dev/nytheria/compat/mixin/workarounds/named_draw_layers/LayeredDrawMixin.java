/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.workarounds.named_draw_layers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.client.gui.LayeredDrawInjection;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

@Mixin(LayeredDraw.class)
public abstract class LayeredDrawMixin implements LayeredDrawInjection {
    @Shadow @Final private List<LayeredDraw.Layer> layers;

    @Unique private final GuiLayerManager nytheriacompat$layerManager = new GuiLayerManager();
    @Unique private final List<ResourceLocation> nytheriacompat$orderedLayerIds = new ArrayList<>();
    @Unique private final Map<ResourceLocation, LayeredDraw.Layer> nytheriacompat$layerMap = new HashMap<>();
    @Unique private final List<LayeredDraw> nytheriacompat$innerDraws = new ArrayList<>();

    @Override
    public GuiLayerManager nytheriacompat$getLayerManager() {
        return this.nytheriacompat$layerManager;
    }

    @Override
    public void nytheriacompat$addVanilla(ResourceLocation id, LayeredDraw.Layer layer) {
        this.nytheriacompat$orderedLayerIds.add(id);
        this.nytheriacompat$layerManager.nytheriacompat$addVanilla(id, layer);
        this.nytheriacompat$layerMap.put(id, layer);
    }

    @Override
    public void nytheriacompat$add(ResourceLocation id) {
        var layer = GuiLayerManager.nytheriacompat$newEmptyLayer();
        this.nytheriacompat$orderedLayerIds.add(id);
        this.nytheriacompat$layerManager.nytheriacompat$addVanilla(id, layer);
        this.nytheriacompat$layerMap.put(id, layer);
        this.layers.add(layer);
    }

    @Override
    public Collection<LayeredDraw> nytheriacompat$getInnerDraws() {
        return this.nytheriacompat$innerDraws;
    }

    @Override
    public Collection<ResourceLocation> nytheriacompat$getOrderedLayerIds() {
        return this.nytheriacompat$orderedLayerIds;
    }

    @Override
    public Map<ResourceLocation, LayeredDraw.Layer> nytheriacompat$getNamedLayers() {
        return this.nytheriacompat$layerMap;
    }

    @Override
    public void nytheriacompat$updateInternalLayers() {
        for (LayeredDraw innerDraw : this.nytheriacompat$innerDraws) {
            innerDraw.nytheriacompat$updateInternalLayers();
        }

        var existing = this.nytheriacompat$layerManager.nytheriacompat$getLayers();

        int currentLayerIndex = 0;
        for (GuiLayerManager.NamedLayer namedLayer : existing) {
            // Set the index of where to start adding layers.
            var actualLayer = this.nytheriacompat$layerMap.get(namedLayer.name());
            if (actualLayer != null) {
                var index = this.layers.indexOf(actualLayer);
                if (index != -1) {
                    currentLayerIndex = index + 1;
                    continue;
                }
            }

            // Avoid double-adding Vanilla layers
            if (this.layers.contains(namedLayer.layer()))
                continue;

            // Otherwise, let's add the layer after <index>.
            if (currentLayerIndex < this.layers.size()) {
                this.layers.add(currentLayerIndex, namedLayer.layer());
            } else {
                this.layers.add(namedLayer.layer());
            }
        }
    }

    @Inject(method = "add(Lnet/minecraft/client/gui/LayeredDraw;Ljava/util/function/BooleanSupplier;)Lnet/minecraft/client/gui/LayeredDraw;", at = @At("HEAD"))
    private void nytheriacompat$registerInternalLayerManagerToOurs(LayeredDraw layeredDraw, BooleanSupplier renderInner, CallbackInfoReturnable<LayeredDraw> cir) {
        var externalManager = layeredDraw.nytheriacompat$getLayerManager();
        this.nytheriacompat$layerManager.add(externalManager, renderInner);
        this.nytheriacompat$layerMap.putAll(layeredDraw.nytheriacompat$getNamedLayers());
        this.nytheriacompat$innerDraws.addAll(layeredDraw.nytheriacompat$getInnerDraws());
        this.nytheriacompat$innerDraws.add(layeredDraw);
        this.nytheriacompat$orderedLayerIds.addAll(layeredDraw.nytheriacompat$getOrderedLayerIds());
    }

    @Unique
    private static boolean nytheriacompat$hasAnyInnerDraw(ResourceLocation id, Collection<LayeredDraw> draws) {
        for (LayeredDraw draw : draws) {
            if (draw.nytheriacompat$getOrderedLayerIds().contains(id))
                return true;

            if (nytheriacompat$hasAnyInnerDraw(id, draw.nytheriacompat$getInnerDraws()))
                return true;
        }

        return false;
    }

    @Override
    public int nytheriacompat$getLayerCount() {
        int count = this.layers.size();

        for (LayeredDraw draw : this.nytheriacompat$innerDraws) {
            count += draw.nytheriacompat$getLayerCount();
        }

        return count;
    }

    @WrapOperation(method = "renderInner", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/LayeredDraw$Layer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void nytheriacompat$tryRenderLayer(LayeredDraw.Layer instance, GuiGraphics guiGraphics, DeltaTracker deltaTracker, Operation<Void> original) {
        var layerManager = this.nytheriacompat$getLayerManager();
        var named = layerManager.nytheriacompat$findNamedLayer(instance);

        if (named != null) {
            // Locate named layer in child draws
            if (nytheriacompat$hasAnyInnerDraw(named.name(), this.nytheriacompat$innerDraws)) {
                // Negate the translation that occurs afterward, we can't exactly wrap blocks of code.
                guiGraphics.pose().translate(0f, 0f, -LayeredDraw.Z_SEPARATION);
                return;
            }

            if (!(EventGate.has(RenderGuiLayerEvent.Pre.class) && NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Pre(guiGraphics, deltaTracker, named.name(), instance)).isCanceled())) {
                original.call(instance, guiGraphics, deltaTracker);
                if (EventGate.has(RenderGuiLayerEvent.Post.class)) NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Post(guiGraphics, deltaTracker, named.name(), instance));
            }
        } else {
            original.call(instance, guiGraphics, deltaTracker);
        }
    }
}
