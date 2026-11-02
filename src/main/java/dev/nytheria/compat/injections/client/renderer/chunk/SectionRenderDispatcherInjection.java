/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.chunk;

import java.util.List;

import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface SectionRenderDispatcherInjection {
    interface RenderSectionInjection {
        interface RebuildTaskInjection {
            default void nytheriacompat$setAdditionalRenderers(List<AddSectionGeometryEvent.AdditionalSectionRenderer> renderers) {
                throw NytheriaCompatHelper.createMixinException(RebuildTaskInjection.class, "nytheriacompat$setAdditionalRenderers");
            }
        }
    }
}
