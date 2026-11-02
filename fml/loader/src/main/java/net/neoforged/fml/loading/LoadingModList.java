/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on FancyModLoader by NeoForged, as ported by KiltMC (see VENDORED.md). Licensed under the GNU LGPL 2.1.
 */

package net.neoforged.fml.loading;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.mojang.logging.LogUtils;
import cpw.mods.modlauncher.api.LambdaExceptionUtils;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.slf4j.Logger;
import dev.nytheria.compat.NytheriaCompat;
import dev.nytheria.compat.loader.mod.fabric.FabricModFileInfoWrapper;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Master list of all mods <em>in the loading context. This class cannot refer outside the
 * loading package</em>
 */
public class LoadingModList {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Nytheria Compat: Rework to point towards Nytheria Compat Loader instead.
    private static final LoadingModList INSTANCE = new LoadingModList();

    public static LoadingModList get() {
        return INSTANCE;
    }

    // Nytheria Compat: Not properly supported, we're not using this.
    public static LoadingModList of(List<ModFile> plugins, List<ModFile> modFiles, List<ModInfo> sortedList, List<ModLoadingIssue> issues, Map<ModInfo, List<ModInfo>> modDependencies) {
        return INSTANCE;
    }

    public List<IModFileInfo> getPlugins() {
        return NytheriaCompat.Companion.getLoader().getMods().stream().map(ModFileInfo::new).map(e -> (IModFileInfo) e).toList();
    }

    public List<ModFileInfo> getModFiles() {
        return NytheriaCompat.Companion.getLoader().getMods().stream().map(ModFileInfo::new).toList();
    }

    public Path findResource(final String className) {
        for (ModFileInfo mf : this.getModFiles()) {
            final Path resource = mf.getFile().findResource(className);
            if (Files.exists(resource)) return resource;
        }
        return null;
    }

    public Enumeration<URL> findAllURLsForResource(final String resName) {
        final String resourceName;
        // strip a leading slash
        if (resName.startsWith("/")) {
            resourceName = resName.substring(1);
        } else {
            resourceName = resName;
        }
        return new Enumeration<URL>() {
            private final Iterator<ModFileInfo> modFileIterator = getModFiles().iterator();
            private URL next;

            @Override
            public boolean hasMoreElements() {
                if (next != null) return true;
                next = findNextURL();
                return next != null;
            }

            @Override
            public URL nextElement() {
                if (next == null) {
                    next = findNextURL();
                    if (next == null) throw new NoSuchElementException();
                }
                URL result = next;
                next = null;
                return result;
            }

            private URL findNextURL() {
                while (modFileIterator.hasNext()) {
                    final ModFileInfo next = modFileIterator.next();
                    final Path resource = next.getFile().findResource(resourceName);
                    if (Files.exists(resource)) {
                        return LambdaExceptionUtils.uncheck(() -> new URL("modjar://" + next.getMods().get(0).getModId() + "/" + resourceName));
                    }
                }
                return null;
            }
        };
    }

    public ModFileInfo getModFileById(String modid) {
        var nytheriaCompatMod = NytheriaCompat.Companion.getLoader().getMod(modid);

        if (nytheriaCompatMod == null) {
            var existingMod = FabricLoader.getInstance().getModContainer(modid);

            if (existingMod.isEmpty()) {
                existingMod = FabricLoader.getInstance().getModContainer(modid.replace("_", "-"));
            }

            if (existingMod.isEmpty()) {
                existingMod = FabricLoader.getInstance().getModContainer(modid.replace("_", ""));
            }

            if (existingMod.isPresent()) {
                return new ModFileInfo(new FabricModFileInfoWrapper(existingMod.orElseThrow()));
            }

            return null;
        }

        return (ModFileInfo) nytheriaCompatMod.getOwningFile();
    }

    public List<ModInfo> getMods() {
        return NytheriaCompat.Companion.getLoader().getMods().stream().map(ModInfo::new).toList();
    }

    public List<ModInfo> getDependencies(IModInfo mod) {
        return List.of(); // Nytheria Compat TODO: is this bad?
    }

    public boolean hasErrors() {
        return false; // Nytheria Compat: We just error in Nytheria Compat's loader itself.
    }

    public List<ModLoadingIssue> getModLoadingIssues() {
        return Collections.emptyList(); // Nytheria Compat: No reason to give this.
    }

    /*
    private static LoadingModList INSTANCE;
    private final List<IModFileInfo> plugins;
    private final List<ModFileInfo> modFiles;
    private final List<ModInfo> sortedList;
    private final Map<ModInfo, List<ModInfo>> modDependencies;
    private final Map<String, ModFileInfo> fileById;
    private final List<ModLoadingIssue> modLoadingIssues;

    private LoadingModList(final List<ModFile> plugins, final List<ModFile> modFiles, final List<ModInfo> sortedList, Map<ModInfo, List<ModInfo>> modDependencies) {
        this.plugins = plugins.stream()
                .map(ModFile::getModFileInfo)
                .collect(Collectors.toList());
        this.modFiles = modFiles.stream()
                .map(ModFile::getModFileInfo)
                .map(ModFileInfo.class::cast)
                .collect(Collectors.toList());
        this.sortedList = sortedList.stream()
                .map(ModInfo.class::cast)
                .collect(Collectors.toList());
        this.modDependencies = modDependencies;
        this.fileById = this.modFiles.stream()
                .map(ModFileInfo::getMods)
                .flatMap(Collection::stream)
                .map(ModInfo.class::cast)
                .collect(Collectors.toMap(ModInfo::getModId, ModInfo::getOwningFile));
        this.modLoadingIssues = new ArrayList<>();
    }

    public static LoadingModList of(List<ModFile> plugins, List<ModFile> modFiles, List<ModInfo> sortedList, List<ModLoadingIssue> issues, Map<ModInfo, List<ModInfo>> modDependencies) {
        INSTANCE = new LoadingModList(plugins, modFiles, sortedList, modDependencies);
        INSTANCE.modLoadingIssues.addAll(issues);
        return INSTANCE;
    }

    public static LoadingModList get() {
        return INSTANCE;
    }

    public void addMixinConfigs() {
        modFiles.stream()
                .map(ModFileInfo::getFile)
                .forEach(file -> {
                    final String modId = file.getModInfos().get(0).getModId();
                    for (ModFileParser.MixinConfig potential : file.getMixinConfigs()) {
                        if (potential.requiredMods().stream().allMatch(id -> this.getModFileById(id) != null)) {
                            DeferredMixinConfigRegistration.addMixinConfig(potential.config(), modId);
                        } else {
                            LOGGER.debug("Mixin config {} for mod {} not applied as required mods are missing", potential.config(), modId);
                        }
                    }
                });
    }

    public void addAccessTransformers() {
        modFiles.stream()
                .map(ModFileInfo::getFile)
                .forEach(mod -> mod.getAccessTransformers().forEach(path -> FMLLoader.addAccessTransformer(path, mod)));
    }

    public void addEnumExtenders() {
        Map<IModInfo, Path> pathPerMod = new HashMap<>();
        modFiles.stream()
                .map(ModFileInfo::getMods)
                .flatMap(List::stream)
                .forEach(mod -> mod.getConfig().<String>getConfigElement("enumExtensions").ifPresent(file -> {
                    Path path = mod.getOwningFile().getFile().findResource(file);
                    if (!Files.isRegularFile(path)) {
                        ModLoader.addLoadingIssue(ModLoadingIssue.error("fml.modloadingissue.enumextender.file_not_found", path).withAffectedMod(mod));
                        return;
                    }
                    pathPerMod.put(mod, path);
                }));
        RuntimeEnumExtender.loadEnumPrototypes(pathPerMod);
    }

    public void addForScanning(BackgroundScanHandler backgroundScanHandler) {
        backgroundScanHandler.setLoadingModList(this);
        modFiles.stream()
                .map(ModFileInfo::getFile)
                .forEach(backgroundScanHandler::submitForScanning);
    }

    public List<IModFileInfo> getPlugins() {
        return plugins;
    }

    public List<ModFileInfo> getModFiles() {
        return modFiles;
    }

    public Path findResource(final String className) {
        for (ModFileInfo mf : modFiles) {
            final Path resource = mf.getFile().findResource(className);
            if (Files.exists(resource)) return resource;
        }
        return null;
    }

    public Enumeration<URL> findAllURLsForResource(final String resName) {
        final String resourceName;
        // strip a leading slash
        if (resName.startsWith("/")) {
            resourceName = resName.substring(1);
        } else {
            resourceName = resName;
        }
        return new Enumeration<URL>() {
            private final Iterator<ModFileInfo> modFileIterator = modFiles.iterator();
            private URL next;

            @Override
            public boolean hasMoreElements() {
                if (next != null) return true;
                next = findNextURL();
                return next != null;
            }

            @Override
            public URL nextElement() {
                if (next == null) {
                    next = findNextURL();
                    if (next == null) throw new NoSuchElementException();
                }
                URL result = next;
                next = null;
                return result;
            }

            private URL findNextURL() {
                while (modFileIterator.hasNext()) {
                    final ModFileInfo next = modFileIterator.next();
                    final Path resource = next.getFile().findResource(resourceName);
                    if (Files.exists(resource)) {
                        return LambdaExceptionUtils.uncheck(() -> new URL("modjar://" + next.getMods().get(0).getModId() + "/" + resourceName));
                    }
                }
                return null;
            }
        };
    }

    public ModFileInfo getModFileById(String modid) {
        return this.fileById.get(modid);
    }

    public List<ModInfo> getMods() {
        return this.sortedList;
    }

    /**
     * Returns all direct loading dependencies of the given mod.
     *
     * <p>This means: all the mods that are directly specified to be loaded before the given mod,
     * either because the given mod has an {@link IModInfo.Ordering#AFTER} constraint on the dependency,
     * or because the dependency has a {@link IModInfo.Ordering#BEFORE} constraint on the given mod.
     */
    /*
    public List<ModInfo> getDependencies(IModInfo mod) {
        return this.modDependencies.getOrDefault(mod, List.of());
    }

    public boolean hasErrors() {
        return !modLoadingIssues.isEmpty() && modLoadingIssues.stream().anyMatch(issue -> issue.severity() == ModLoadingIssue.Severity.ERROR);
    }

    public List<ModLoadingIssue> getModLoadingIssues() {
        return modLoadingIssues;
    }
    */
}
