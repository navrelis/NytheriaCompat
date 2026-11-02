/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.gametest.framework;

import net.minecraft.gametest.framework.GameTestRegistry;

import java.lang.reflect.Method;
import java.util.Set;

public interface GameTestRegistryInjection {
    static void register(Method method, Set<String> allowedNamespaces) {
        GameTestRegistry.register(method);
    }
}
