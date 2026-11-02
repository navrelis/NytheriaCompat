// TRACKED HASH: c9ae7f6c9276aa62194bc83803132dfe3cad1b05
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects;

import net.minecraft.CrashReportCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.CrashReportCategoryInjection;

@Mixin(CrashReportCategory.class)
public class CrashReportCategoryInject implements CrashReportCategoryInjection {
    @Shadow private StackTraceElement[] stackTrace;

    @Override
    public void setStackTrace(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }
}