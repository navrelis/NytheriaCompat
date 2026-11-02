/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.resources.model;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public interface ModelResourceLocationInjection {
    static String STANDALONE_VARIANT = "standalone";

    static ModelResourceLocation standalone(ResourceLocation id) {
        return new ModelResourceLocation(id, STANDALONE_VARIANT);
    }
}
