/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;


@FabricInjectedInterface(DataGenerator.class)
public interface DataGeneratorInjection {
    default PackOutput getPackOutput() {
        throw new IllegalStateException();
    }
    default PackOutput getPackOutput(String path) {
        throw new IllegalStateException();
    }
    default <T extends DataProvider> T addProvider(boolean run, DataProvider.Factory<T> factory) {
        throw new IllegalStateException();
    }
    default <T extends DataProvider> T addProvider(boolean run, T provider) {
        throw new IllegalStateException();
    }
    default void merge(DataGenerator other) {
        throw NytheriaCompatHelper.createMixinException(DataGeneratorInjection.class, "merge");
    }
}
