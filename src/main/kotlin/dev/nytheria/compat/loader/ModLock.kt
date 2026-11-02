package dev.nytheria.compat.loader

/**
 * Nytheria Compat: the terms Kilt's author agreed to. Nytheria Compat is an internal dependency of the Nytheria modpack, so it loads only the
 * NeoForge mods below and needs the pack's core mods. There is no way around this check, also not in a development environment.
 */
object ModLock {
    // NeoForge mods Nytheria Compat may load. Also the registry namespaces of dev.nytheria.compat.hook.Scope.
    @JvmField val ALLOWED_NEOFORGE_MODS = setOf("cataclysm", "astral_dimension", "lionfishapi", "curios")

    // Fabric mods that must be present, any version. A group needs one of its ids; questbridge is the old id of nytheriaquests.
    val REQUIRED_FABRIC_MODS = listOf(listOf("nytheriaquests", "questbridge"), listOf("tickmender"))

    const val REFUSED_TEXT = "Nytheria Compat only runs inside the Nytheria modpack; to run other NeoForge mods on Fabric, use Kilt, https://github.com/KiltMC/Kilt."

    /** A mod id from a `neoforge.mods.toml`. [fabricJar]: Fabric Loader runs this jar as a Fabric mod, Nytheria Compat does not load it. */
    data class FoundMod(val jar: String, val modId: String, val fabricJar: Boolean)

    /** What is wrong with this set of mods, one line per problem. Empty means the game may start. */
    fun check(found: List<FoundMod>, fabricModIds: Collection<String>): List<String> {
        val refused = found.filter { !it.fabricJar && it.modId !in ALLOWED_NEOFORGE_MODS }
            .map { "NeoForge mod ${it.modId} in ${it.jar} is not allowed." }
        val missing = REQUIRED_FABRIC_MODS.filter { group -> group.none { it in fabricModIds } }
            .map { "Required Fabric mod ${it.first()}${it.drop(1).joinToString("") { id -> " (or $id)" }} is missing." }

        return refused + missing
    }
}
