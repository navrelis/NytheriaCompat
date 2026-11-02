/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.api.remapping

/**
 * Allows mods to define what they want to remap specific names to.
 * Useful in the cases of conflicting field/method signatures, such as
 * Forge mods having getHelperName()Ljava/lang/String;
 * while Fabric mods having getHelperName()Lsome/mod/StringImpl;
 */
interface InsnConflictRemapProvider {
    fun remapField(owner: String, name: String, descriptor: String): String {
        return name
    }

    fun remapMethod(owner: String, name: String, descriptor: String): String {
        return name
    }
}