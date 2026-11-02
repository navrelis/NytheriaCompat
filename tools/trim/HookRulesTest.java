package dev.nytheria.compat.trim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import dev.nytheria.compat.hook.EventGate;
import dev.nytheria.compat.hook.Need;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Rules H2, H5 and H7 of the 2.0.0 design over Nytheria Compat's compiled classes: no {@code @Overwrite}, no {@code @Redirect} on a
 * vanilla class, every {@code NeoForge.EVENT_BUS.post} has an {@code EventGate.has} before it in the same method, and no
 * extension interface a mixin implements re-declares a method of a Fabric API or vanilla interface.
 *
 * <p>Reads class files as bytes, so comments and Kotlin make no difference and no game class is loaded. It compares what it
 * finds with {@code hook-rules-allowed.txt}: a new violation fails, and so does an entry that no longer exists, which keeps
 * the list shrinking until it is empty. Each run writes what it found to {@code build/hook-rules-found.txt}.
 *
 * <p>It also holds the hooks against {@code event-need.tsv}: every event gated by {@code EventGate.has} has a row, and every
 * event narrowed by a {@link Need} method has a {@code NARROW} row that names that method, a handler and a proof.
 */
public class HookRulesTest {
    private static final List<Path> CLASSES = List.of(Path.of("build/classes/java/main/dev/nytheria/compat"), Path.of("build/classes/kotlin/main/dev/nytheria/compat"));
    private static final Path ALLOWED = Path.of("tools/trim/hook-rules-allowed.txt");
    private static final Path FOUND = Path.of("build/hook-rules-found.txt");
    private static final Path HOOKS = Path.of("build/classes/java/main/net/neoforged/neoforge");
    private static final Path NEED_TABLE = Path.of("tools/trim/event-need.tsv");

    private static final String MIXIN = Type.getDescriptor(Mixin.class);
    private static final String OVERWRITE = Type.getDescriptor(Overwrite.class);
    private static final String REDIRECT = Type.getDescriptor(Redirect.class);
    private static final String EVENT_GATE = Type.getInternalName(EventGate.class);
    private static final String NEED = Type.getInternalName(Need.class);
    private static final String NEOFORGE = Type.getInternalName(NeoForge.class);
    private static final String EVENT_BUS_API = "net/neoforged/bus/api/IEventBus";

    // ---- the check ------------------------------------------------------------------------------------------------

    private static void scan(byte[] bytes, Set<String> found) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        boolean vanilla = targetsVanilla(node);
        for (MethodNode method : node.methods) {
            String where = node.name + "#" + method.name + method.desc;
            if (annotation(method, OVERWRITE) != null) found.add("overwrite " + where);
            if (vanilla && annotation(method, REDIRECT) != null) found.add("redirect " + where);
            if (postsWithoutGate(method)) found.add("post " + where);
        }
        redeclared(node, found);
    }

    // ---- H7: an extension interface must not re-declare a method of a Fabric API or vanilla interface ---------------

    private static final Map<String, ClassNode> LOADED = new HashMap<>();

    private static ClassNode load(String name) {
        ClassNode loaded = LOADED.get(name);
        if (loaded == null) {
            try (InputStream in = HookRulesTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
                if (in == null) return null;
                loaded = new ClassNode();
                new ClassReader(in).accept(loaded, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                LOADED.put(name, loaded);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return loaded;
    }

    /** The interfaces a class or interface has, through its superclasses and interfaces. */
    private static void interfacesOf(String type, Set<String> into) {
        ClassNode node = load(type);
        if (node == null) return;
        if (node.superName != null) interfacesOf(node.superName, into);
        for (String name : node.interfaces) {
            if (into.add(name)) interfacesOf(name, into);
        }
    }

    private static boolean external(String type) {
        return type.startsWith("net/fabricmc/") || type.startsWith("net/minecraft/");
    }

    private static Set<String> instanceMethods(String type) {
        Set<String> methods = new TreeSet<>();
        ClassNode node = load(type);
        if (node == null) return methods;
        for (MethodNode method : node.methods) {
            if ((method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC)) == 0 && !method.name.startsWith("<")) {
                methods.add(method.name + method.desc);
            }
        }
        return methods;
    }

    /**
     * A mixin that implements one of our interfaces gives the target class that interface's defaults. If the interface declares
     * a method a Fabric API or vanilla interface of the target (or of the interface itself) declares too, a block that gets both
     * through different paths fails with a conflicting-default error (the crash of 2026-10-19, getAppearance).
     */
    private static void redeclared(ClassNode mixin, Set<String> found) {
        Set<String> targetInterfaces = new TreeSet<>();
        for (String target : mixinTargets(mixin)) interfacesOf(target, targetInterfaces);
        for (String ours : mixin.interfaces) {
            if (!ours.startsWith("dev/nytheria/compat/") && !ours.startsWith("net/neoforged/")) continue;
            Set<String> others = new TreeSet<>(targetInterfaces);
            interfacesOf(ours, others);
            others.remove(ours);
            Set<String> declared = instanceMethods(ours);
            for (String other : others) {
                if (!external(other)) continue;
                for (String method : instanceMethods(other)) {
                    if (declared.contains(method)) found.add("redeclare " + ours + "#" + method + " " + other);
                }
            }
        }
    }

    private static List<String> mixinTargets(ClassNode node) {
        List<String> targets = new ArrayList<>();
        AnnotationNode mixin = null;
        if (node.invisibleAnnotations != null) mixin = find(node.invisibleAnnotations, MIXIN);
        if (mixin == null && node.visibleAnnotations != null) mixin = find(node.visibleAnnotations, MIXIN);
        if (mixin == null || mixin.values == null) return targets;
        for (int i = 0; i < mixin.values.size(); i += 2) {
            if (!(mixin.values.get(i + 1) instanceof List<?> list)) continue;
            for (Object target : list) {
                targets.add(target instanceof Type type ? type.getInternalName() : String.valueOf(target).replace('.', '/'));
            }
        }
        return targets;
    }

    private static boolean postsWithoutGate(MethodNode method) {
        boolean bus = false;
        boolean gate = false;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof FieldInsnNode field && field.owner.equals(NEOFORGE) && field.name.equals("EVENT_BUS")) bus = true;
            if (insn instanceof MethodInsnNode call) {
                if (call.owner.equals(EVENT_GATE)) gate = true;
                if (bus && !gate && call.owner.equals(EVENT_BUS_API) && call.name.equals("post")) return true;
            }
        }
        return false;
    }

    private static boolean targetsVanilla(ClassNode node) {
        AnnotationNode mixin = null;
        if (node.invisibleAnnotations != null) mixin = find(node.invisibleAnnotations, MIXIN);
        if (mixin == null && node.visibleAnnotations != null) mixin = find(node.visibleAnnotations, MIXIN);
        if (mixin == null || mixin.values == null) return false;
        for (int i = 0; i < mixin.values.size(); i += 2) {
            if (!(mixin.values.get(i + 1) instanceof List<?> targets)) continue;
            for (Object target : targets) {
                String name = target instanceof Type type ? type.getInternalName() : String.valueOf(target).replace('.', '/');
                if (name.startsWith("net/minecraft/") || name.startsWith("com/mojang/")) return true;
            }
        }
        return false;
    }

    private static AnnotationNode annotation(MethodNode method, String desc) {
        AnnotationNode found = method.visibleAnnotations == null ? null : find(method.visibleAnnotations, desc);
        return found != null || method.invisibleAnnotations == null ? found : find(method.invisibleAnnotations, desc);
    }

    private static AnnotationNode find(List<AnnotationNode> annotations, String desc) {
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(desc)) return annotation;
        }
        return null;
    }

    private static List<String> problems(Set<String> found, Set<String> allowed) {
        List<String> problems = new ArrayList<>();
        for (String entry : found) {
            if (!allowed.contains(entry)) problems.add("new violation: " + entry);
        }
        for (String entry : allowed) {
            if (!found.contains(entry)) problems.add("gone, take it off " + ALLOWED + ": " + entry);
        }
        return problems;
    }

    /** What the hooks ask: every event gated, every event with its {@link Need} method (the event as named in the table). */
    private record Hooks(Set<String> gated, Map<String, Set<String>> needs, Set<String> needMethods) {
        Hooks() {
            this(new TreeSet<>(), new TreeMap<>(), new TreeSet<>());
        }
    }

    private static void scanHooks(byte[] bytes, Hooks hooks) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (node.name.equals(NEED)) {
            for (MethodNode method : node.methods) {
                if ((method.access & Opcodes.ACC_PUBLIC) != 0) hooks.needMethods.add(method.name);
            }
            return;
        }
        for (MethodNode method : node.methods) {
            String constant = null;
            String gate = null;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type type) constant = type.getInternalName();
                if (insn instanceof MethodInsnNode call) {
                    if (call.owner.equals(EVENT_GATE) && call.name.equals("has")) {
                        gate = constant;
                        hooks.gated.add(tableName(gate));
                    }
                    if (call.owner.equals(NEED)) {
                        hooks.needs.computeIfAbsent(gate == null ? "no gate before Need in " + node.name : tableName(gate), k -> new TreeSet<>()).add(call.name);
                    }
                }
            }
        }
    }

    private static String tableName(String event) {
        return event.replace("net/neoforged/neoforge/", "").replace('/', '.');
    }

    /** A table row by event: listeners, need, condition, proof, verdict. */
    private record Row(String listeners, String need, String condition, String proof, String verdict) {
    }

    private static Map<String, Row> readTable(List<String> lines) {
        Map<String, Row> rows = new TreeMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] c = line.split("\t", -1);
            rows.put(c[0], new Row(c[2], c[4], c[6], c[7], c[8]));
        }
        return rows;
    }

    private static boolean none(String cell) {
        return cell.isBlank() || cell.equals("-");
    }

    private static List<String> tableProblems(Hooks hooks, Map<String, Row> rows) {
        List<String> problems = new ArrayList<>();
        for (String event : hooks.gated) {
            if (!rows.containsKey(event)) problems.add("no row for gated event " + event);
        }
        Set<String> used = new TreeSet<>();
        hooks.needs.forEach((event, methods) -> {
            used.addAll(methods);
            Row row = rows.get(event);
            if (row == null) {
                problems.add("no row for narrowed event " + event);
            } else if (!row.verdict.equals("NARROW") || !methods.equals(Set.of(row.need))) {
                problems.add("row does not say NARROW by " + methods + ": " + event);
            }
        });
        for (String method : hooks.needMethods) {
            if (!used.contains(method)) problems.add("no hook calls Need." + method);
        }
        rows.forEach((event, row) -> {
            boolean listened = !none(row.listeners);
            switch (row.verdict) {
                case "NARROW" -> {
                    if (!hooks.needs.containsKey(event)) problems.add("NARROW row without a Need call: " + event);
                    if (!listened || none(row.need) || none(row.condition) || none(row.proof)) problems.add("NARROW row without handler, need, condition or proof: " + event);
                }
                case "ALWAYS" -> {
                    if (!listened) problems.add("ALWAYS row without a handler: " + event);
                }
                case "NONE" -> {
                    if (listened) problems.add("NONE row with a handler: " + event);
                }
                default -> problems.add("unknown verdict: " + event);
            }
        });
        return problems;
    }

    // ---- the rule over Nytheria Compat ------------------------------------------------------------------------------------

    @Test
    public void nytheriaCompatKeepsTheRules() throws IOException {
        Set<String> found = new TreeSet<>();
        for (Path root : CLASSES) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                    scan(Files.readAllBytes(file), found);
                }
            }
        }
        Files.write(FOUND, found);

        Set<String> allowed = new TreeSet<>();
        for (String line : Files.readAllLines(ALLOWED)) {
            if (!line.isBlank() && !line.startsWith("#")) allowed.add(line);
        }
        List<String> problems = problems(found, allowed);
        assertTrue(problems.isEmpty(), () -> String.join("\n", problems.subList(0, Math.min(20, problems.size()))));
    }

    @Test
    public void everyNeedHasItsRow() throws IOException {
        Hooks hooks = new Hooks();
        for (Path root : List.of(CLASSES.get(0), CLASSES.get(1), HOOKS)) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                    scanHooks(Files.readAllBytes(file), hooks);
                }
            }
        }
        assertEquals(19, hooks.needs.size(), () -> "narrowed events: " + hooks.needs.keySet());
        List<String> problems = tableProblems(hooks, readTable(Files.readAllLines(NEED_TABLE)));
        assertTrue(problems.isEmpty(), () -> String.join("\n", problems.subList(0, Math.min(20, problems.size()))));
    }

    // ---- every injector has a row in hook-need.tsv ------------------------------------------------------------------

    private static final Path HOOK_NEED = Path.of("tools/trim/hook-need.tsv");
    private static final List<String> INJECTORS = List.of(
        "Lorg/spongepowered/asm/mixin/Overwrite;", "Lorg/spongepowered/asm/mixin/injection/Inject;", "Lorg/spongepowered/asm/mixin/injection/Redirect;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyArg;", "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;", "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;",
        "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;", "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
        "Lcom/llamalad7/mixinextras/injector/ModifyReceiver;", "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;",
        "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;", "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;",
        "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;");

    private static void scanInjectors(byte[] bytes, Set<String> found) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
        for (MethodNode method : node.methods) {
            if (INJECTORS.stream().anyMatch(desc -> annotation(method, desc) != null)) {
                found.add("src/main/java/" + node.name.split("\\$")[0] + ".java\t" + method.name);
            }
        }
    }

    /** {@code hook-reach.py} writes the table; a row of kind {@code note} is a finding that is no injector. */
    @Test
    public void everyInjectorHasARow() throws IOException {
        Set<String> found = new TreeSet<>();
        for (String folder : List.of("injects", "mixin")) {
            try (Stream<Path> files = Files.walk(CLASSES.get(0).resolve(folder))) {
                for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                    scanInjectors(Files.readAllBytes(file), found);
                }
            }
        }

        Set<String> rows = new TreeSet<>();
        for (String line : Files.readAllLines(HOOK_NEED).subList(1, Files.readAllLines(HOOK_NEED).size())) {
            String[] cells = line.split("\t", -1);
            if (!cells[3].equals("note")) rows.add(cells[0] + "\t" + cells[1]);
        }

        List<String> problems = new ArrayList<>();
        for (String injector : found) {
            if (!rows.contains(injector)) problems.add("no row, run tools/trim/hook-reach.py: " + injector);
        }
        for (String row : rows) {
            if (!found.contains(row)) problems.add("stale row, run tools/trim/hook-reach.py: " + row);
        }
        assertTrue(problems.isEmpty(), () -> String.join("\n", problems.subList(0, Math.min(20, problems.size()))));
    }

    // ---- the check over fixtures ----------------------------------------------------------------------------------

    public static class TestEvent extends Event {
    }

    @Mixin(net.minecraft.server.MinecraftServer.class)
    public static class VanillaMixin {
        @Overwrite
        public void overwritten() {
        }

        @Redirect(method = "tickServer", at = @At("HEAD"))
        public void redirected() {
        }
    }

    @Mixin(targets = "net.minecraft.server.MinecraftServer")
    public static class VanillaByNameMixin {
        @Redirect(method = "tickServer", at = @At("HEAD"))
        public void redirected() {
        }
    }

    @Mixin(targets = "me.jellysquid.mods.sodium.client.Sodium")
    public static class OtherModMixin {
        @Redirect(method = "tick", at = @At("HEAD"))
        public void redirected() {
        }

        @Overwrite
        public void overwritten() {
        }
    }

    /** Declares {@code ItemLike#asItem} again: an item that also gets {@code ItemLike} from vanilla has two defaults. */
    public interface ReDeclaring {
        default net.minecraft.world.item.Item asItem() {
            return null;
        }
    }

    public interface Fresh {
        default int nytheriaCompatFreshMethod() {
            return 0;
        }
    }

    @Mixin(net.minecraft.world.item.Item.class)
    public static class RedeclaringMixin implements ReDeclaring {
    }

    @Mixin(net.minecraft.world.item.Item.class)
    public static class FreshMixin implements Fresh {
    }

    public static class Posts {
        public static void bare() {
            NeoForge.EVENT_BUS.post(new TestEvent());
        }

        public static void gated() {
            if (EventGate.has(TestEvent.class)) NeoForge.EVENT_BUS.post(new TestEvent());
        }

        public static void gatedTooLate() {
            NeoForge.EVENT_BUS.post(new TestEvent());
            EventGate.has(TestEvent.class);
        }

        public static void otherBus(net.neoforged.bus.api.IEventBus modBus) {
            modBus.post(new TestEvent());
        }

        public static void narrowed(net.minecraft.world.entity.player.Player player) {
            if (EventGate.has(TestEvent.class) && Need.interact(player)) NeoForge.EVENT_BUS.post(new TestEvent());
        }

        public static void needWithoutGate(net.minecraft.world.entity.player.Player player) {
            Need.interact(player);
        }
    }

    private static Set<String> scan(Class<?> type) throws IOException {
        Set<String> found = new TreeSet<>();
        try (InputStream in = type.getClassLoader().getResourceAsStream(Type.getInternalName(type) + ".class")) {
            scan(in.readAllBytes(), found);
        }
        return found;
    }

    @Test
    public void findsOverwriteAndRedirectOnVanilla() throws IOException {
        String name = Type.getInternalName(VanillaMixin.class);
        assertEquals(Set.of("overwrite " + name + "#overwritten()V", "redirect " + name + "#redirected()V"), scan(VanillaMixin.class));
    }

    @Test
    public void findsRedirectOnVanillaTargetByName() throws IOException {
        assertEquals(Set.of("redirect " + Type.getInternalName(VanillaByNameMixin.class) + "#redirected()V"), scan(VanillaByNameMixin.class));
    }

    @Test
    public void redirectOnAnotherModIsFineButOverwriteIsNot() throws IOException {
        assertEquals(Set.of("overwrite " + Type.getInternalName(OtherModMixin.class) + "#overwritten()V"), scan(OtherModMixin.class));
    }

    @Test
    public void findsPostsWithoutGateOnTheGameBus() throws IOException {
        String name = Type.getInternalName(Posts.class);
        assertEquals(Set.of("post " + name + "#bare()V", "post " + name + "#gatedTooLate()V"), scan(Posts.class));
    }

    private static final String TEST_EVENT = tableName(Type.getInternalName(TestEvent.class));
    private static final String NO_GATE = "no gate before Need in " + Type.getInternalName(Posts.class);
    private static final Row NARROW = new Row("cataclysm:Handler#method", "interact", "stun", "reads stun", "NARROW");

    private static Hooks hooks(String event) {
        Hooks hooks = new Hooks();
        hooks.gated.add(event);
        hooks.needs.put(event, Set.of("interact"));
        hooks.needMethods.add("interact");
        return hooks;
    }

    @Test
    public void readsNeedCallsFromTheHooks() throws IOException {
        Hooks hooks = new Hooks();
        try (InputStream in = Posts.class.getClassLoader().getResourceAsStream(Type.getInternalName(Posts.class) + ".class")) {
            scanHooks(in.readAllBytes(), hooks);
        }
        assertEquals(Set.of(TEST_EVENT), hooks.gated);
        assertEquals(Map.of(TEST_EVENT, Set.of("interact"), NO_GATE, Set.of("interact")), hooks.needs);
    }

    @Test
    public void aFullRowIsFine() {
        assertEquals(List.of(), tableProblems(hooks("e"), Map.of("e", NARROW)));
    }

    @Test
    public void aNarrowedEventWithoutARowFails() {
        assertEquals(List.of("no row for gated event e", "no row for narrowed event e"), tableProblems(hooks("e"), Map.of()));
    }

    @Test
    public void aRowWithoutAHandlerFails() {
        Row row = new Row("-", "interact", "stun", "reads stun", "NARROW");
        assertEquals(List.of("NARROW row without handler, need, condition or proof: e"), tableProblems(hooks("e"), Map.of("e", row)));
    }

    @Test
    public void aRowOfTheWrongNeedFails() {
        Row row = new Row(NARROW.listeners, "blockBreak", "stun", "reads stun", "NARROW");
        assertEquals(List.of("row does not say NARROW by [interact]: e"), tableProblems(hooks("e"), Map.of("e", row)));
    }

    @Test
    public void aRowLeftBehindFails() {
        assertEquals(List.of("NARROW row without a Need call: gone"), tableProblems(hooks("e"), Map.of("e", NARROW, "gone", NARROW)));
    }

    @Test
    public void aNeedNoHookCallsFails() {
        Hooks hooks = hooks("e");
        hooks.needMethods.add("unused");
        assertEquals(List.of("no hook calls Need.unused"), tableProblems(hooks, Map.of("e", NARROW)));
    }

    @Test
    public void findsInterfaceThatRedeclaresAVanillaInterfaceMethod() throws IOException {
        assertEquals(Set.of("redeclare " + Type.getInternalName(ReDeclaring.class) + "#asItem()Lnet/minecraft/world/item/Item; net/minecraft/world/level/ItemLike"),
                scan(RedeclaringMixin.class));
    }

    @Test
    public void interfaceWithNewMethodsIsFine() throws IOException {
        assertEquals(Set.of(), scan(FreshMixin.class));
    }

    @Test
    public void newAndStaleEntriesAreProblems() {
        assertEquals(List.of("new violation: a", "gone, take it off " + ALLOWED + ": c"), problems(Set.of("a", "b"), Set.of("b", "c")));
    }
}
