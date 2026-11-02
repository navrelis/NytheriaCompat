/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

/**
 * Mixin extensions used by Nytheria Compat that have no good reason to be used literally anywhere else.
 * Any element created using these extensions will be completely inaccessible to the source code
 * (unless you use reflection), but are still accessible by the Forge mods.
 * <p>
 * There is no good reason for these extensions to be used by any other mod, and thus, they are
 * only provided by Nytheria Compat. However, feel free to throw the code here into your own mod if you do manage
 * to find a good reason to use these.
 */
package dev.nytheria.compat.helpers.mixin;