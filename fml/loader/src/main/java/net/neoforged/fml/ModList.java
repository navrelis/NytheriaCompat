/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on FancyModLoader by NeoForged, as ported by KiltMC (see VENDORED.md). Licensed under the GNU LGPL 2.1.
 */

package net.neoforged.fml;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import kotlin.collections.CollectionsKt;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.IModFile;
import dev.nytheria.compat.NytheriaCompat;
import dev.nytheria.compat.loader.mod.NeoForgeMod;
import dev.nytheria.compat.loader.mod.fabric.WrappedFabricModContainer;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Master list of all mods - game-side version. This is classloaded in the game scope and
 * can dispatch game level events as a result.
 */
public class ModList {
    private static ModList INSTANCE = new ModList();
    /*
    private final List<IModFileInfo> modFiles;
    private final List<IModInfo> sortedList;
    private final Map<String, ModFileInfo> fileById;
    private List<ModContainer> mods;
    private Map<String, ModContainer> indexedMods;
    private List<ModFileScanData> modFileScanData;
    private List<ModContainer> sortedContainers;

    private ModList(final List<ModFile> modFiles, final List<ModInfo> sortedList) {
        this.modFiles = modFiles.stream().map(ModFile::getModFileInfo).map(ModFileInfo.class::cast).collect(Collectors.toList());
        this.sortedList = sortedList.stream().map(ModInfo.class::cast).collect(Collectors.toList());
        this.fileById = this.modFiles.stream().map(IModFileInfo::getMods).flatMap(Collection::stream).map(ModInfo.class::cast).collect(Collectors.toMap(ModInfo::getModId, ModInfo::getOwningFile));
        CrashReportCallables.registerCrashCallable("Mod List", this::crashReport);
    }

    private String fileToLine(IModFile mf) {
        var mainMod = mf.getModInfos().getFirst();
        return String.format(Locale.ENGLISH, "%-50.50s|%-30.30s|%-30.30s|%-20.20s|Manifest: %s", mf.getFileName(),
                mainMod.getDisplayName(),
                mainMod.getModId(),
                mainMod.getVersion(),
                ((ModFileInfo) mf.getModFileInfo()).getCodeSigningFingerprint().orElse("NOSIGNATURE"));
    }

    private String crashReport() {
        return "\n" + applyForEachModFileAlphabetical(this::fileToLine).collect(Collectors.joining("\n\t\t", "\t\t", ""));
    }*/

    public static ModList of(List<ModFile> modFiles, List<ModInfo> sortedList) {
        //INSTANCE = new ModList(modFiles, sortedList); // Nytheria Compat: idc
        return INSTANCE;
    }

    public static ModList get() {
        return INSTANCE;
    }

    public List<IModFileInfo> getModFiles() {
        // Nytheria Compat: redirect here, it just works, don't question it.
        return LoadingModList.get().getPlugins();
        //return modFiles;
    }

    public IModFileInfo getModFileById(String modid) {
        // Nytheria Compat: redirect here, it just works, don't question it.
        return LoadingModList.get().getModFileById(modid);
//        return this.fileById.get(modid);
    }

    static CompletionStage<Void> completableFutureFromExceptionList(List<? extends Map.Entry<?, Throwable>> t) {
        if (t.stream().noneMatch(e -> e.getValue() != null)) {
            return CompletableFuture.completedFuture(null);
        } else {
            final List<Throwable> throwables = t.stream().filter(e -> e.getValue() != null).map(Map.Entry::getValue).collect(Collectors.toList());
            CompletableFuture<Void> cf = new CompletableFuture<>();
            final RuntimeException accumulator = new RuntimeException();
            cf.completeExceptionally(accumulator);
            throwables.forEach(exception -> {
                if (exception instanceof CompletionException) {
                    exception = exception.getCause();
                }
                if (exception.getSuppressed().length != 0) {
                    Arrays.stream(exception.getSuppressed()).forEach(accumulator::addSuppressed);
                } else {
                    accumulator.addSuppressed(exception);
                }
            });
            return cf;
        }
    }

    static <V> CompletableFuture<List<Map.Entry<V, Throwable>>> gather(List<? extends CompletableFuture<? extends V>> futures) {
        List<Map.Entry<V, Throwable>> list = new ArrayList<>(futures.size());
        CompletableFuture<?>[] results = new CompletableFuture[futures.size()];
        futures.forEach(future -> {
            int i = list.size();
            list.add(null);
            results[i] = future.whenComplete((result, exception) -> list.set(i, new AbstractMap.SimpleImmutableEntry<>(result, exception)));
        });
        return CompletableFuture.allOf(results).handle((r, th) -> null).thenApply(res -> list);
    }

    void setLoadedMods(final List<ModContainer> modContainers) {
        /*
        this.mods = modContainers;
        this.sortedContainers = modContainers.stream().sorted(Comparator.comparingInt(c -> sortedList.indexOf(c.getModInfo()))).toList();
        this.indexedMods = modContainers.stream().collect(Collectors.toMap(ModContainer::getModId, Function.identity()));
        */
    }

    public Optional<? extends ModContainer> getModContainerById(String modId) {
        // Nytheria Compat: Point to loader
        return Optional.ofNullable(NytheriaCompat.Companion.getLoader().getMod(modId)).map(NeoForgeMod::getContainer)
            .or(() -> FabricLoader.getInstance().getModContainer(modId).map(WrappedFabricModContainer::get))
            .or(() -> FabricLoader.getInstance().getModContainer(modId.replace("_", "-")).map(WrappedFabricModContainer::get));
//        return Optional.ofNullable(this.indexedMods.get(modId));
    }

    public List<IModInfo> getMods() {
        // Nytheria Compat: Point to loader
        return NytheriaCompat.Companion.getLoader().getMods().stream().map(e -> (IModInfo) e).toList();
//        return this.sortedList;
    }

    public boolean isLoaded(String modTarget) {
        // Nytheria Compat: Point to loader
        return NytheriaCompat.Companion.getLoader().hasMod(modTarget) || FabricLoader.getInstance().isModLoaded(modTarget)
            || FabricLoader.getInstance().isModLoaded(modTarget.replace("_", "-")); // Cloth Config detection, probably

//        return this.indexedMods.containsKey(modTarget);
    }

    public int size() {
        // Nytheria Compat: Point to loader
        return NytheriaCompat.Companion.getLoader().getMods().size();

//        return mods.size();
    }

    public List<ModFileScanData> getAllScanData() {
        /*
        if (modFileScanData == null) {
            modFileScanData = this.sortedList.stream().map(IModInfo::getOwningFile).filter(Objects::nonNull).map(IModFileInfo::getFile).distinct().map(IModFile::getScanResult).collect(Collectors.toList());
        }
        return modFileScanData;
         */

        return NytheriaCompat.Companion.getLoader().getMods().stream().map(NeoForgeMod::getScanData).toList();
    }

    public void forEachModFile(Consumer<IModFile> fileConsumer) {
//        modFiles.stream().map(IModFileInfo::getFile).forEach(fileConsumer);
        NytheriaCompat.Companion.getLoader().getMods().stream().map(e -> e.getOwningFile().getFile()).forEach(fileConsumer);
    }

    public <T> Stream<T> applyForEachModFile(Function<IModFile, T> function) {
//        return modFiles.stream().map(IModFileInfo::getFile).map(function);
        return NytheriaCompat.Companion.getLoader().getMods().stream().map(e -> e.getOwningFile().getFile()).map(function);
    }

    /**
     * Stream sorted by Mod Name in alphabetical order
     */
    public <T> Stream<T> applyForEachModFileAlphabetical(Function<IModFile, T> function) {
        return NytheriaCompat.Companion.getLoader().getMods().stream().map(e -> e.getOwningFile().getFile())
                .sorted(Comparator.comparing(modFile -> modFile.getModInfos().getFirst().getDisplayName(), String.CASE_INSENSITIVE_ORDER))
                .map(function);
    }

    public void forEachModContainer(BiConsumer<String, ModContainer> modContainerConsumer) {
        NytheriaCompat.Companion.getLoader().getMods().stream().map(NeoForgeMod::getContainer).forEach(container -> {
            modContainerConsumer.accept(container.getModId(), container);
        });
//        indexedMods.forEach(modContainerConsumer);
    }

    public List<ModContainer> getSortedMods() {
//        return sortedContainers;
        return CollectionsKt.plus(WrappedFabricModContainer.getWrappedContainers(), NytheriaCompat.Companion.getLoader().getMods().stream().map(NeoForgeMod::getContainer).toList());
    }

    public void forEachModInOrder(Consumer<ModContainer> containerConsumer) {
        this.getSortedMods().forEach(containerConsumer);
    }

    public <T> Stream<T> applyForEachModContainer(Function<ModContainer, T> function) {
        return this.getSortedMods().stream().map(function);
    }
}
