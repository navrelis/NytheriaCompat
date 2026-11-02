/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer;

import java.util.function.Consumer;

public interface RenderPropertiesInjection<T> {
    // Forge's comments say to call RenderProperties.get, but there's a problem.
    // wHERE THE FUCK IS IT
    // In the meantime, let's just do this.
    default Object getRenderPropertiesInternal() {
        throw new IllegalStateException();
    }

    // This is a funny little workaround to make this work for everything
    default void initializeClient(Consumer<T> consumer) {}
}
