/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.config;

import java.nio.file.Path;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.jetbrains.annotations.Nullable;

public record LoadedConfig(CommentedConfig config, @Nullable Path path, ModConfig modConfig) implements IConfigSpec.ILoadedConfig {
    @Override
    public void save() {
        if (path != null) {
            ConfigTracker.writeConfig(path, config);
        }
        modConfig.lock.lock();
        try {
            modConfig.container.acceptEvent(new ModConfigEvent.Reloading(modConfig));
        } finally {
            modConfig.lock.unlock();
        }
    }
}
