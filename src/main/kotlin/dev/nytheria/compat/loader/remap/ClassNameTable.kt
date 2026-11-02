package dev.nytheria.compat.loader.remap

/**
 * Nytheria Compat: the class names of a tiny v2 file, both ways. The class-only lookups (remapClass, unmapClass, remapDescriptor) run on
 * every start, and a full remap cache must not need the parsed member mappings just for them. Members are indented, so a class is
 * a line that starts with `c`. An inner class without a mapping of its own follows its outer class, like IMappingFile.remapClass.
 */
internal class ClassNameTable(lines: Sequence<String>) {
    private val forward = HashMap<String, String>()
    private val reverse = HashMap<String, String>()

    init {
        for (line in lines) {
            if (!line.startsWith("c\t"))
                continue

            val split = line.split('\t')
            forward[split[1]] = split[2]
            reverse[split[2]] = split[1]
        }
    }

    fun map(name: String): String = lookup(forward, name)

    fun unmap(name: String): String = lookup(reverse, name)

    private fun lookup(names: Map<String, String>, name: String): String {
        names[name]?.let { return it }

        val inner = name.lastIndexOf('$')
        return if (inner == -1) name else lookup(names, name.substring(0, inner)) + name.substring(inner)
    }
}
