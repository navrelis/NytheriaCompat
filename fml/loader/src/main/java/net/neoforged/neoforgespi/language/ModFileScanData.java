/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on FancyModLoader by NeoForged, as ported by KiltMC (see VENDORED.md). Licensed under the GNU LGPL 2.1.
 */

package net.neoforged.neoforgespi.language;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.objectweb.asm.Type;

public class ModFileScanData {
    private final Set<AnnotationData> annotations = new LinkedHashSet<>();
    private final Set<ClassData> classes = new LinkedHashSet<>();
    private final List<IModFileInfo> modFiles = new ArrayList<>();

    public Set<ClassData> getClasses() {
        return classes;
    }

    public Set<AnnotationData> getAnnotations() {
        return annotations;
    }

    public Stream<AnnotationData> getAnnotatedBy(Class<? extends Annotation> type, ElementType elementType) {
        final var anType = Type.getType(type);
        return getAnnotations().stream()
                .filter(ad -> ad.targetType == elementType && ad.annotationType.equals(anType));
    }

    public void addModFileInfo(IModFileInfo info) {
        this.modFiles.add(info);
    }

    public List<IModFileInfo> getIModInfoData() {
        return this.modFiles;
    }

    public void nytheriacompat$copyFrom(ModFileScanData scanData) {
        this.annotations.addAll(scanData.annotations);
        this.classes.addAll(scanData.classes);
    }

    public record ClassData(Type clazz, Type parent, Set<Type> interfaces) {}

    public record AnnotationData(Type annotationType, ElementType targetType, Type clazz, String memberName, Map<String, Object> annotationData) {}
}
