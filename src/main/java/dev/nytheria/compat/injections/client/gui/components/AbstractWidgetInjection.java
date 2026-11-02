/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.gui.components;

import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface AbstractWidgetInjection {
    int UNSET_FG_COLOR = -1;

    default int getFGColor() {
        throw NytheriaCompatHelper.createMixinException(AbstractWidgetInjection.class, "getFGColor");
    }

}
