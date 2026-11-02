/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Processed by Nytheria Compat's Gradle scripts to automatically be written to the fabric.mod.json for injected interfaces.
 * Must still be applied manually via mixin in order to work correctly!
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface FabricInjectedInterface {
    /**
     * Represents the exact class that is to be injected onto.
     * @return The class to be injected onto
     */
    Class<?>[] value() default {};

    /**
     * For use in cases where the class is inaccessible in some way, and an access widener is undesirable.
     * @return The fully qualified class name
     */
    String[] target() default {};
}
