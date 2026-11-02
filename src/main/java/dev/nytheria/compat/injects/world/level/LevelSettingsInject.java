// TRACKED HASH: 9f12b57e97b354c71b054480f97d5c1e96fc46ae
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level;

import com.mojang.serialization.Lifecycle;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.world.level.LevelSettingsInjection;

@Mixin(LevelSettings.class)
public abstract class LevelSettingsInject implements LevelSettingsInjection {
    @Shadow @Final private String levelName;
    @Shadow @Final private GameType gameType;
    @Shadow @Final private boolean hardcore;
    @Shadow @Final private Difficulty difficulty;
    @Shadow @Final private boolean allowCommands;
    @Shadow @Final private GameRules gameRules;
    @Shadow @Final private WorldDataConfiguration dataConfiguration;
    @Unique private Lifecycle lifecycle = Lifecycle.stable();

    public LevelSettingsInject(String levelName, GameType gameType, boolean hardcore, Difficulty difficulty, boolean allowCommands, GameRules gameRules, WorldDataConfiguration dataConfiguration) {}

    @CreateInitializer
    public LevelSettingsInject(String levelName, GameType gameType, boolean hardcore, Difficulty difficulty, boolean allowCommands, GameRules gameRules, WorldDataConfiguration dataConfiguration, Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void nytheriacompat$setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public LevelSettings withLifecycle(Lifecycle lifecycle) {
        var settings = new LevelSettings(this.levelName, this.gameType, this.hardcore, this.difficulty, this.allowCommands, this.gameRules, this.dataConfiguration);
        ((LevelSettingsInjection) (Object) settings).nytheriacompat$setLifecycle(lifecycle);

        return settings;
    }
}