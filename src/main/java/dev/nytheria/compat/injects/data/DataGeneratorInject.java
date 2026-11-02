// TRACKED HASH: de027ad643273d383b040eb1533f1634a1ff65cd
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.data.DataGeneratorInjection;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

@Mixin(DataGenerator.class)
public class DataGeneratorInject implements DataGeneratorInjection {
    @Shadow @Final private Map<String, DataProvider> providersToRun;
    @Shadow @Final private PackOutput vanillaPackOutput;
    @Shadow @Final private Path rootOutputFolder;
    @Shadow @Final private Set<String> allProviderIds;


    @Override
    public PackOutput getPackOutput() {
        return this.vanillaPackOutput;
    }

    @Override
    public PackOutput getPackOutput(String path) {
        return new PackOutput(this.rootOutputFolder.resolve(path));
    }

    @Override
    public <T extends DataProvider> T addProvider(boolean run, DataProvider.Factory<T> factory) {
        return addProvider(run, factory.create(this.vanillaPackOutput));
    }

    @Override
    public <T extends DataProvider> T addProvider(boolean run, T provider) {
        var id = provider.getName();

        if (!this.allProviderIds.add(id))
            throw new IllegalStateException("Duplicate provider: " + id);

        if (run)
            this.providersToRun.put(id, provider);

        return provider;
    }
}