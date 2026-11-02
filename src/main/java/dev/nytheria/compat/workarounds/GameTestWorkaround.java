/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds;

public @interface GameTestWorkaround {
    int timeoutTicks() default 100;

    String batch() default "defaultBatch";

    int rotationSteps() default 0;

    boolean required() default true;

    String template() default "";

    long setupTicks() default 0L;

    int attempts() default 1;

    int requiredSuccesses() default 1;

    String templateNamespace() default "";
}
