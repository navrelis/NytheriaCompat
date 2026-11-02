/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.remaps.fml.config

import net.neoforged.fml.config.IConfigSpec
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.ModContainer
import dev.nytheria.compat.loader.NytheriaCompatModContainer

/*open class ModConfigRemap : ModConfig {
    constructor(type: Type, spec: IConfigSpec<*>, activeContainer: ModContainer) : super(type, spec, (activeContainer as NytheriaCompatModContainer).modId)
    constructor(type: Type, spec: IConfigSpec<*>, activeContainer: ModContainer, fileName: String) : super(type, spec, (activeContainer as NytheriaCompatModContainer).modId, fileName)
}*/