/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.helpers.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used internally by Nytheria Compat for adding new static fields and methods that are inaccessible within
 * Forge/Nytheria Compat itself, but are used in Forge mods as a result of Forge's patches.
 * <p>
 * This functions by adding the annotated static field/method in the mixin into the actual class,
 * and promoting it to be public.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CreateStatic {
}
