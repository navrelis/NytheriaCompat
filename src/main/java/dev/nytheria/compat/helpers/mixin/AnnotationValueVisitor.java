/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.helpers.mixin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

public class AnnotationValueVisitor extends AnnotationVisitor {
    public AnnotationValueVisitor() {
        super(Opcodes.ASM9);
    }

    public final Map<String, Object> values = new HashMap<>();

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
        this.values.put(name, value);
    }
}
