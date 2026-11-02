package dev.nytheria.compat.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Nytheria Compat: The Aether for Fabric (and its library Nitrogen Internals) ports parts of NeoForge and registers them under
//  the "neoforge" namespace, which collides with the real NeoForge that Nytheria Compat bundles
//  (https://github.com/The-Aether-Team/Nitrogen/issues/38). The mixins in dev.nytheria.compat.mixin.compat.nitrogen move
//  those ids to the "nitrogen_internals" namespace through here.
public final class NitrogenCompat {
    public static final String NEOFORGE_NAMESPACE = "neoforge";
    public static final String NITROGEN_NAMESPACE = "nitrogen_internals";

    private static final Logger LOGGER = LoggerFactory.getLogger("Nytheria Compat");
    private static volatile boolean logged = false;

    private NitrogenCompat() {}

    /** "neoforge" -> "nitrogen_internals", anything else is returned as is. */
    public static String remapNamespace(String namespace) {
        if (!NEOFORGE_NAMESPACE.equals(namespace))
            return namespace;

        logOnce();
        return NITROGEN_NAMESPACE;
    }

    /** "neoforge:path" -> "nitrogen_internals:path", anything else is returned as is. */
    public static String remapId(String id) {
        if (id == null || !id.startsWith(NEOFORGE_NAMESPACE + ":"))
            return id;

        logOnce();
        return NITROGEN_NAMESPACE + id.substring(NEOFORGE_NAMESPACE.length());
    }

    private static void logOnce() {
        if (logged)
            return;

        logged = true;
        LOGGER.info("Nytheria Compat: moving The Aether / Nitrogen Internals' own copies of NeoForge network ids from the \"neoforge\" namespace to \"nitrogen_internals\", so they don't collide with NeoForge");
    }
}
