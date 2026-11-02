/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader

object NytheriaCompatFlags {
    // Mainly for debugging, so already-remapped Forge mods will be remapped again.
    @JvmField val FORCE_REMAPPING = "nytheriacompat.forceRemap".checkPropertyBoolean()

    // Mainly for debugging, used to test unobfuscated mods and ensure that Nytheria Compat is running as intended.
    @JvmField val DISABLE_REMAPPING = "nytheriacompat.noRemap".checkPropertyBoolean()

    // Mainly for debugging, to make sure all Forge mods remap correctly in production environments
    // without needing to actually launch a production environment.
    @JvmField val FORCE_PRODUCTION_REMAPPING = FORCE_REMAPPING && "nytheriacompat.forceProductionRemap".checkPropertyBoolean()

    // Mainly for debugging, enables profiling if the DeltaTimeProfiler#dumpTree method is called.
    // No longer does anything.
    //@JvmField val ENABLE_PROFILING = "nytheriacompat.enableProfiling".checkPropertyBoolean()

    // Mainly for debugging, enables logging access transformer info under the INFO level.
    // By default, AT info is logged under the DEBUG level, so it may still be found there.
    @JvmField val ENABLE_ACCESS_TRANSFORMER_DEBUG = "nytheriacompat.printATDebug".checkPropertyBoolean()

    // Nytheria Compat: logs the startup phase timings (dev.nytheria.compat.util.NytheriaCompatTimings) under the INFO level instead of DEBUG.
    @JvmField val PRINT_TIMINGS = "nytheriacompat.printTimings".checkPropertyBoolean()

    // Nytheria Compat: NeoForge data (neoforge:overlays, neoforge:conditions next to fabric:load_conditions, biome/structure
    // modifiers, global loot modifier lists, data maps) is only applied from the packs of the NeoForge mods Nytheria Compat runs
    // and Nytheria Compat's own packs; Fabric mods, datapacks and every other pack get plain Fabric's behaviour (see
    // dev.nytheria.compat.util.NeoForgeSources). This flag makes Nytheria Compat apply that data from every pack again, as earlier
    // versions did.
    @JvmField val NEO_DATA_FROM_FABRIC_MODS = "nytheriacompat.neoDataFromFabricMods".checkPropertyBoolean()

    // The names of Sporran 1.0.8 are read too, so a launcher that still passes them keeps its options. rename:keep
    private const val OLD_PROPERTY_PREFIX = "sporran." // rename:keep

    private fun String.checkPropertyBoolean(): Boolean {
        return System.getProperty(this)?.lowercase() == "true"
            || System.getProperty(OLD_PROPERTY_PREFIX + removePrefix("nytheria_compat."))?.lowercase() == "true"
    }
}