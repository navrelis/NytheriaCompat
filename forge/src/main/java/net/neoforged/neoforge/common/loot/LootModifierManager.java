/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on NeoForge by NeoForged, as ported by KiltMC (see VENDORED.md). Licensed under the GNU LGPL 2.1.
 */

package net.neoforged.neoforge.common.loot;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dev.nytheria.compat.util.NeoForgeSources;
import dev.nytheria.compat.workarounds.ContextAwareReloadListenerWorkaround;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public class LootModifierManager extends SimpleJsonResourceReloadListener implements ContextAwareReloadListenerWorkaround {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Logger LOGGER = LogManager.getLogger();

    private Map<ResourceLocation, IGlobalLootModifier> registeredLootModifiers = ImmutableMap.of();
    private static final String folder = "loot_modifiers";

    public LootModifierManager() {
        super(GSON, folder);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<ResourceLocation, JsonElement> map = super.prepare(resourceManager, profilerFiller);
        List<ResourceLocation> finalLocations = new ArrayList<>();
        ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath("neoforge", "loot_modifiers/global_loot_modifiers.json");
        // Nytheria Compat: only the lists of NeoForge mods (and Nytheria Compat's own) take part in the layering; the others, their entries
        // and their "replace", are left alone as on plain Fabric (Porting Lib's own manager still reads them for Fabric ports)
        NeoForgeSources.Ignored ignored = new NeoForgeSources.Ignored("NeoForge global loot modifier lists from packs that are not NeoForge mods (plain Fabric ignores them; Porting Lib's manager still reads them)");
        List<Resource> layers = NeoForgeSources.keepNeoForgeSources(resourceManager.getResourceStack(resourceLocation), Resource::sourcePackId, NeoForgeSources::ignoresNeoForgeData, resourceLocation, ignored);
        ignored.log();
        //read in all data files from neoforge:loot_modifiers/global_loot_modifiers in order to do layering
        for (Resource resource : layers) {
            try (Reader reader = resource.openAsReader()) {
                JsonObject jsonobject = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                boolean replace = GsonHelper.getAsBoolean(jsonobject, "replace", false);
                if (replace)
                    finalLocations.clear();
                JsonArray entries = GsonHelper.getAsJsonArray(jsonobject, "entries");
                for (int i = 0; i < entries.size(); i++) {
                    ResourceLocation loc = ResourceLocation.parse(GsonHelper.convertToString(entries.get(i), "entries[" + i + "]"));
                    finalLocations.remove(loc); //remove and re-add if needed, to update the ordering.
                    finalLocations.add(loc);
                }
            } catch (RuntimeException | IOException ioexception) {
                LOGGER.error("Couldn't read global loot modifier list {} in data pack {}", resourceLocation, resource.sourcePackId(), ioexception);
            }
        }
        Map<ResourceLocation, JsonElement> finalMap = new HashMap<>();
        //use layered config to fetch modifier data files (modifiers missing from config are disabled)
        for (ResourceLocation location : finalLocations) {
            // Nytheria Compat: Otherwise Tempad causes problems, we have to make sure the elements actually exist.
            JsonElement element = map.get(location);
            if (element == null) continue;

            finalMap.put(location, element);
        }
        return finalMap;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceList, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        DynamicOps<JsonElement> ops = this.nytheriacompat$asContextAware().makeConditionalOps();
        Builder<ResourceLocation, IGlobalLootModifier> builder = ImmutableMap.builder();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceList.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement json = entry.getValue();
            IGlobalLootModifier.CONDITIONAL_CODEC.parse(ops, json)
                    // log error if parse fails
                    .resultOrPartial(errorMsg -> LOGGER.warn("Could not decode GlobalLootModifier with json id {} - error: {}", location, errorMsg))
                    // add loot modifier if parse succeeds
                    .flatMap(Function.identity())
                    .ifPresent(carrier -> builder.put(location, carrier.carrier()));
        }
        this.registeredLootModifiers = builder.build();
    }

    /**
     * An immutable collection of the registered loot modifiers in layered order.
     */
    public Collection<IGlobalLootModifier> getAllLootMods() {
        return registeredLootModifiers.values();
    }
}
