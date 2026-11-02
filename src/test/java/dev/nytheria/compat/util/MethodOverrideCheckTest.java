package dev.nytheria.compat.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * {@link NytheriaCompatHelper#hasMethodOverride}, {@link NytheriaCompatHelper#hasMethodOverrideWithReturnType} and
 * {@link NytheriaCompatHelper.MethodOverrideCheck} have to give the answer of the plain reflection lookup they cache
 * ({@link #reference}), for every class and every check, whichever of them is asked first and from however many threads.
 *
 * <p>These are plain unit tests: nothing here starts Fabric Loader or touches Minecraft. {@link NytheriaCompatHelper} itself
 * cannot be initialised without a running loader, so its two methods are called through
 * {@link #hasMethodOverride} and {@link #hasMethodOverrideWithReturnType}, which do exactly what they do.
 */
public class MethodOverrideCheckTest {
    // ---- fixtures -------------------------------------------------------------------------------------------------

    public interface Extension {
        default int extended(String value) {
            return 0;
        }

        default Number typed() {
            return 0;
        }

        default void untouched() {
        }
    }

    public interface SubExtension extends Extension {
        @Override
        default int extended(String value) {
            return 3;
        }
    }

    public static class Base implements Extension {
        public void plain(int value) {
        }

        public void plain(int value, String other) {
        }

        public Object returned() {
            return null;
        }

        protected void guarded() {
        }
    }

    /** Overrides directly. */
    public static class Direct extends Base {
        @Override
        public void plain(int value) {
        }
    }

    /** Overrides a class method and an interface default method. */
    public static class Middle extends Base {
        @Override
        public void plain(int value, String other) {
        }

        @Override
        public int extended(String value) {
            return 1;
        }
    }

    /** Overrides nothing itself, its superclass does. */
    public static class Leaf extends Middle {
    }

    /** Overrides nothing at all. */
    public static class Untouched extends Base {
    }

    /** Same name, other arguments: not an override. */
    public static class Overloaded extends Base {
        public void plain(long value) {
        }

        private void plain(String value) {
        }

        @SuppressWarnings("unused")
        private void hidden() {
        }
    }

    /** Overrides with a narrower return type, so the class has the method twice (String and the Object bridge). */
    public static class Covariant extends Base {
        @Override
        public String returned() {
            return "";
        }

        @Override
        public Integer typed() {
            return 1;
        }
    }

    /** Gets the override of a default method from another interface. */
    public static class ThroughInterface extends Base implements SubExtension {
    }

    /** Does not extend Base at all. */
    public static class Unrelated {
        public void plain(int value) {
        }
    }

    public abstract static class AbstractTop extends Base {
        @Override
        public abstract void plain(int value);
    }

    // ---- fixtures for the declared (protected method) checks --------------------------------------------------------

    /** Like the NeoForge additions to vanilla classes: the hooks are protected, so Class#getMethod cannot see them. */
    public static class Guarded {
        protected void hook(int value) {
        }

        protected void hook(int value, String other) {
        }
    }

    /** Overrides the protected method. */
    public static class GuardedOverride extends Guarded {
        @Override
        protected void hook(int value) {
        }
    }

    /** Overrides nothing. */
    public static class GuardedPlain extends Guarded {
    }

    /** Overrides while the class in between does not. */
    public static class GuardedGrandOverride extends GuardedPlain {
        @Override
        protected void hook(int value) {
        }
    }

    /** Overrides nothing itself, its superclass does. */
    public static class GuardedGrandInherit extends GuardedOverride {
    }

    /** Has methods of the same signature as hooks that are not overrides (they are not in Guarded, so private is legal). */
    public static class GuardedHidden extends Guarded {
        @SuppressWarnings("unused")
        private void secret(int value) {
        }

        @SuppressWarnings("unused")
        static void util(int value) {
        }
    }

    /** Overrides a protected method that is generic in the superclass, so the class also has a synthetic bridge method. */
    public static class GenericGuarded<T> {
        protected void accept(T value) {
        }
    }

    public static class StringGuarded extends GenericGuarded<String> {
        @Override
        protected void accept(String value) {
        }
    }

    /** Has a protected method of the same name and arguments but is no Guarded. */
    public static class GuardedLookalike {
        protected void hook(int value) {
        }
    }

    public interface Hook {
        default void fire(int value) {
        }

        default void fireLater(int value) {
        }
    }

    public interface SubHook extends Hook {
        @Override
        default void fire(int value) {
        }

        @Override
        default void fireLater(int value) {
        }
    }

    /** Gets the override of Hook only from an interface, so Class#getMethod finds it and no class declares it. */
    public static class HookImpl implements SubHook {
    }

    private static final List<Class<?>> TOP_CLASSES = List.of(
        Base.class, Direct.class, Middle.class, Leaf.class, Untouched.class, Overloaded.class, Covariant.class,
        ThroughInterface.class, Unrelated.class, AbstractTop.class, Extension.class, SubExtension.class,
        Object.class, String.class, int.class, Base[].class
    );

    private static final List<Class<?>> SUPER_CLASSES = List.of(Base.class, Extension.class, SubExtension.class, Middle.class, Object.class);

    /** The body of {@link NytheriaCompatHelper#hasMethodOverride}. */
    private static boolean hasMethodOverride(Class<?> topClass, Class<?> superClass, String methodName, Class<?>... methodArgs) {
        return NytheriaCompatHelper.MethodOverrideCache.INSTANCE.hasMethodOverride(topClass, superClass, methodName, methodArgs, null);
    }

    /** The body of {@link NytheriaCompatHelper#hasDeclaredMethodOverride}. */
    private static boolean hasDeclaredMethodOverride(Class<?> topClass, Class<?> superClass, String methodName, Class<?>... methodArgs) {
        return NytheriaCompatHelper.MethodOverrideCache.INSTANCE.hasDeclaredMethodOverride(topClass, superClass, methodName, methodArgs);
    }

    /** The body of {@link NytheriaCompatHelper#hasMethodOverrideWithReturnType}. */
    private static boolean hasMethodOverrideWithReturnType(Class<?> topClass, Class<?> superClass, String methodName, Class<?> returnType, Class<?>... methodArgs) {
        return NytheriaCompatHelper.MethodOverrideCache.INSTANCE.hasMethodOverride(topClass, superClass, methodName, methodArgs, returnType);
    }

    /** A check: what is looked up, without the class it is looked up for. */
    private record Check(Class<?> superClass, String methodName, Class<?> returnType, Class<?>... methodArgs) {
        boolean viaHelper(Class<?> topClass) {
            return this.returnType == null
                ? hasMethodOverride(topClass, this.superClass, this.methodName, this.methodArgs.clone())
                : hasMethodOverrideWithReturnType(topClass, this.superClass, this.methodName, this.returnType, this.methodArgs.clone());
        }

        NytheriaCompatHelper.MethodOverrideCheck prepared() {
            return this.returnType == null
                ? NytheriaCompatHelper.MethodOverrideCheck.of(this.superClass, this.methodName, this.methodArgs.clone())
                : NytheriaCompatHelper.MethodOverrideCheck.withReturnType(this.superClass, this.methodName, this.returnType, this.methodArgs.clone());
        }

        boolean reference(Class<?> topClass) {
            return MethodOverrideCheckTest.reference(topClass, this.superClass, this.methodName, this.returnType, this.methodArgs);
        }

        @Override
        public String toString() {
            return this.superClass.getSimpleName() + "#" + this.methodName + Arrays.toString(this.methodArgs) + (this.returnType == null ? "" : " -> " + this.returnType.getSimpleName());
        }
    }

    /**
     * The lookup that the helper caches, written out with plain reflection and no cache: the public method of that
     * name and those argument types (and, if asked for, that return type) that the class ends up with counts as an
     * override unless it is still the one declared by the given superclass or interface.
     */
    private static boolean reference(Class<?> topClass, Class<?> superClass, String methodName, Class<?> returnType, Class<?>... methodArgs) {
        Method method = null;

        if (returnType == null) {
            try {
                method = topClass.getMethod(methodName, methodArgs);
            } catch (NoSuchMethodException e) {
                return false;
            }
        } else {
            for (Method candidate : topClass.getMethods()) {
                if (candidate.getName().equals(methodName) && Arrays.equals(candidate.getParameterTypes(), methodArgs) && candidate.getReturnType() == returnType) {
                    method = candidate;
                    break;
                }
            }

            if (method == null) {
                return false;
            }
        }

        return method.getDeclaringClass() != superClass && !Modifier.isPrivate(method.getModifiers());
    }

    private static List<Check> checksFor(List<Class<?>> superClasses) {
        List<Check> checks = new ArrayList<>();

        for (Class<?> superClass : superClasses) {
            for (Class<?> returnType : new Class<?>[] { null, void.class, int.class, Object.class, String.class, Number.class, Integer.class }) {
                checks.add(new Check(superClass, "plain", returnType, int.class));
                checks.add(new Check(superClass, "plain", returnType, int.class, String.class));
                checks.add(new Check(superClass, "plain", returnType, long.class));
                checks.add(new Check(superClass, "plain", returnType, String.class));
                checks.add(new Check(superClass, "plain", returnType));
                checks.add(new Check(superClass, "plain", returnType, Integer.class));
                checks.add(new Check(superClass, "plain", returnType, String.class, int.class));
                checks.add(new Check(superClass, "extended", returnType, String.class));
                checks.add(new Check(superClass, "extended", returnType, Object.class));
                checks.add(new Check(superClass, "typed", returnType));
                checks.add(new Check(superClass, "untouched", returnType));
                checks.add(new Check(superClass, "returned", returnType));
                checks.add(new Check(superClass, "guarded", returnType));
                checks.add(new Check(superClass, "hidden", returnType));
                checks.add(new Check(superClass, "missing", returnType));
                checks.add(new Check(superClass, "toString", returnType));
                checks.add(new Check(superClass, "equals", returnType, Object.class));
            }
        }

        return checks;
    }

    // ---- the cases the helper exists for -------------------------------------------------------------------------

    @Test
    public void knownCases() {
        // overridden directly
        assertTrue(hasMethodOverride(Direct.class, Base.class, "plain", int.class));
        // overridden in an intermediate superclass
        assertTrue(hasMethodOverride(Leaf.class, Base.class, "plain", int.class, String.class));
        // not overridden
        assertFalse(hasMethodOverride(Untouched.class, Base.class, "plain", int.class));
        assertFalse(hasMethodOverride(Base.class, Base.class, "plain", int.class));
        assertFalse(hasMethodOverride(Leaf.class, Base.class, "plain", int.class));
        // an overload or a private method of that name is not an override
        assertFalse(hasMethodOverride(Overloaded.class, Base.class, "plain", int.class));
        assertFalse(hasMethodOverride(Overloaded.class, Base.class, "plain", String.class));
        // no such method at all
        assertFalse(hasMethodOverride(Direct.class, Base.class, "missing"));
        // interface default methods
        assertFalse(hasMethodOverride(Base.class, Extension.class, "extended", String.class));
        assertFalse(hasMethodOverride(Direct.class, Extension.class, "extended", String.class));
        assertTrue(hasMethodOverride(Middle.class, Extension.class, "extended", String.class));
        assertTrue(hasMethodOverride(Leaf.class, Extension.class, "extended", String.class));
        assertTrue(hasMethodOverride(ThroughInterface.class, Extension.class, "extended", String.class));
        assertFalse(hasMethodOverride(ThroughInterface.class, SubExtension.class, "extended", String.class));
        // different return types
        assertTrue(hasMethodOverrideWithReturnType(Covariant.class, Base.class, "returned", String.class));
        assertTrue(hasMethodOverrideWithReturnType(Covariant.class, Base.class, "returned", Object.class));
        assertFalse(hasMethodOverrideWithReturnType(Covariant.class, Base.class, "returned", Integer.class));
        assertFalse(hasMethodOverrideWithReturnType(Untouched.class, Base.class, "returned", Object.class));
        assertFalse(hasMethodOverrideWithReturnType(Untouched.class, Base.class, "returned", String.class));
        assertTrue(hasMethodOverrideWithReturnType(Covariant.class, Extension.class, "typed", Integer.class));
        assertFalse(hasMethodOverrideWithReturnType(Base.class, Extension.class, "typed", Number.class));
        assertFalse(hasMethodOverrideWithReturnType(Direct.class, Base.class, "plain", int.class, int.class));
        assertTrue(hasMethodOverrideWithReturnType(Direct.class, Base.class, "plain", void.class, int.class));

        // the prepared check answers the same
        assertTrue(NytheriaCompatHelper.MethodOverrideCheck.of(Base.class, "plain", int.class).test(Direct.class));
        assertFalse(NytheriaCompatHelper.MethodOverrideCheck.of(Base.class, "plain", int.class).test(Untouched.class));
        assertTrue(NytheriaCompatHelper.MethodOverrideCheck.withReturnType(Base.class, "returned", String.class).test(Covariant.class));
        assertFalse(NytheriaCompatHelper.MethodOverrideCheck.withReturnType(Base.class, "returned", String.class).test(Untouched.class));
    }

    // ---- declared checks (hasDeclaredMethodOverride, MethodOverrideCheck.ofDeclared) ------------------------------------

    @Test
    public void declaredFindsProtectedOverrides() {
        // overridden directly, protected
        assertTrue(hasDeclaredMethodOverride(GuardedOverride.class, Guarded.class, "hook", int.class));
        // not overridden
        assertFalse(hasDeclaredMethodOverride(GuardedPlain.class, Guarded.class, "hook", int.class));
        assertFalse(hasDeclaredMethodOverride(GuardedPlain.class, Guarded.class, "hook", int.class, String.class));
        // overridden in the class below while the one in between does not
        assertTrue(hasDeclaredMethodOverride(GuardedGrandOverride.class, Guarded.class, "hook", int.class));
        assertFalse(hasDeclaredMethodOverride(GuardedGrandOverride.class, Guarded.class, "hook", int.class, String.class));
        // inherited override
        assertTrue(hasDeclaredMethodOverride(GuardedGrandInherit.class, Guarded.class, "hook", int.class));
        // declared only in the superclass: not an override, for the superclass itself as well
        assertFalse(hasDeclaredMethodOverride(Guarded.class, Guarded.class, "hook", int.class));
        // no such method, other arguments
        assertFalse(hasDeclaredMethodOverride(GuardedOverride.class, Guarded.class, "missing", int.class));
        assertFalse(hasDeclaredMethodOverride(GuardedOverride.class, Guarded.class, "hook", long.class));
        // private and static methods of that name are no overrides
        assertFalse(hasDeclaredMethodOverride(GuardedHidden.class, Guarded.class, "secret", int.class));
        assertFalse(hasDeclaredMethodOverride(GuardedHidden.class, Guarded.class, "util", int.class));
        // not a subclass of the superclass at all
        assertFalse(hasDeclaredMethodOverride(GuardedLookalike.class, Guarded.class, "hook", int.class));
        assertFalse(hasDeclaredMethodOverride(Object.class, Guarded.class, "hook", int.class));
        assertFalse(hasDeclaredMethodOverride(int.class, Guarded.class, "hook", int.class));
        // the synthetic bridge accept(Object) only exists because StringGuarded overrides the generic method, so it counts
        assertTrue(hasDeclaredMethodOverride(StringGuarded.class, GenericGuarded.class, "accept", Object.class));
        assertTrue(hasDeclaredMethodOverride(StringGuarded.class, GenericGuarded.class, "accept", String.class));

        // the prepared check answers the same, also when it is asked first
        NytheriaCompatHelper.MethodOverrideCheck prepared = NytheriaCompatHelper.MethodOverrideCheck.ofDeclared(Guarded.class, "hook", int.class);

        assertTrue(prepared.test(GuardedOverride.class));
        assertTrue(prepared.test(GuardedGrandOverride.class));
        assertTrue(prepared.test(GuardedGrandInherit.class));
        assertFalse(prepared.test(GuardedPlain.class));
        assertFalse(prepared.test(Guarded.class));
        assertFalse(prepared.test(GuardedLookalike.class));
        assertTrue(NytheriaCompatHelper.MethodOverrideCheck.ofDeclared(GenericGuarded.class, "accept", String.class).test(StringGuarded.class));
    }

    @Test
    public void declaredAndPublicChecksDoNotShareResults() {
        // The public lookup cannot see a protected method at all, which is why the declared one exists.
        assertFalse(hasMethodOverride(GuardedOverride.class, Guarded.class, "hook", int.class));
        assertTrue(hasDeclaredMethodOverride(GuardedOverride.class, Guarded.class, "hook", int.class));
        assertFalse(NytheriaCompatHelper.MethodOverrideCheck.of(Guarded.class, "hook", int.class).test(GuardedOverride.class));
        assertTrue(NytheriaCompatHelper.MethodOverrideCheck.ofDeclared(Guarded.class, "hook", int.class).test(GuardedOverride.class));

        // The other way round: found through an interface, where no class declares the method. Asked public first for
        // "fire" and declared first for "fireLater", so neither order may decide the answer of the other.
        assertTrue(hasMethodOverride(HookImpl.class, Hook.class, "fire", int.class));
        assertFalse(hasDeclaredMethodOverride(HookImpl.class, Hook.class, "fire", int.class));
        assertFalse(hasDeclaredMethodOverride(HookImpl.class, Hook.class, "fireLater", int.class));
        assertTrue(hasMethodOverride(HookImpl.class, Hook.class, "fireLater", int.class));
        assertTrue(NytheriaCompatHelper.MethodOverrideCheck.of(Hook.class, "fire", int.class).test(HookImpl.class));
        assertFalse(NytheriaCompatHelper.MethodOverrideCheck.ofDeclared(Hook.class, "fire", int.class).test(HookImpl.class));

        // Both kinds for the same class, name and arguments are different cache entries.
        NytheriaCompatHelper.OverrideData publicKey = new NytheriaCompatHelper.OverrideData(Guarded.class, "hook", new Class<?>[] { int.class }, null, false);
        NytheriaCompatHelper.OverrideData declaredKey = new NytheriaCompatHelper.OverrideData(Guarded.class, "hook", new Class<?>[] { int.class }, null, true);

        assertFalse(publicKey.equals(declaredKey));
        assertNotSame(
            NytheriaCompatHelper.MethodOverrideCache.INSTANCE.getOverrideClassValue(publicKey),
            NytheriaCompatHelper.MethodOverrideCache.INSTANCE.getOverrideClassValue(declaredKey)
        );
        assertSame(
            NytheriaCompatHelper.MethodOverrideCache.INSTANCE.getOverrideClassValue(declaredKey),
            NytheriaCompatHelper.MethodOverrideCache.INSTANCE.getOverrideClassValue(new NytheriaCompatHelper.OverrideData(Guarded.class, "hook", new Class<?>[] { int.class }, null, true))
        );
    }

    @Test
    public void everyCombinationMatchesReflection() {
        List<Check> checks = checksFor(SUPER_CLASSES);
        int overridden = 0;

        // Three passes, so that every check is asked while it is new and again once it is cached, and the table of the
        // helper has grown several times in between. The prepared check is asked first for every other check.
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < checks.size(); i++) {
                Check check = checks.get(i);
                NytheriaCompatHelper.MethodOverrideCheck prepared = check.prepared();

                for (Class<?> topClass : TOP_CLASSES) {
                    boolean expected = check.reference(topClass);
                    boolean preparedFirst = (i + pass) % 2 == 0;

                    if (preparedFirst) {
                        assertEquals(expected, prepared.test(topClass), () -> "prepared " + check + " for " + topClass);
                    }

                    assertEquals(expected, check.viaHelper(topClass), () -> "helper " + check + " for " + topClass);
                    assertEquals(expected, prepared.test(topClass), () -> "prepared " + check + " for " + topClass);
                    assertEquals(expected, check.prepared().test(topClass), () -> "second prepared " + check + " for " + topClass);

                    if (expected && pass == 0) {
                        overridden++;
                    }
                }
            }
        }

        // The fixtures really cover both answers.
        assertTrue(overridden > 50, "only " + overridden + " overrides in the fixtures");
        assertLookupTableIsConsistent();
    }

    /**
     * Checks that differ in exactly one part must never be taken for each other, also when they share a slot of the
     * lookup table. Whether two checks share a slot depends on identity hash codes, so each family is large enough
     * that many of its members do; the method names with equal hash codes share one for certain.
     */
    @Test
    public void lookupTellsApartChecksThatDifferInOnePart() throws Exception {
        List<Class<?>> pool = new ArrayList<>();

        for (Class<?> element : List.of(Object.class, String.class, Integer.class, Base.class, Direct.class, Extension.class, int.class, long.class)) {
            Class<?> type = element;

            for (int dimensions = 1; dimensions <= 250; dimensions++) {
                type = type.arrayType();
                pool.add(type);
            }
        }

        List<Check> checks = new ArrayList<>();

        for (Class<?> type : pool) {
            checks.add(new Check(type, "plain", null, int.class));          // only the superclass differs
            checks.add(new Check(Base.class, "typed", type));               // only the return type differs
            checks.add(new Check(Base.class, "plain", null, int.class, type)); // only the last argument differs
            checks.add(new Check(Base.class, "plain", null, type, int.class)); // only the first argument differs
        }

        Class<?>[] repeated = new Class<?>[40];
        Arrays.fill(repeated, int.class);

        for (int count = 0; count <= repeated.length; count++) {
            checks.add(new Check(Base.class, "plain", null, Arrays.copyOf(repeated, count))); // only the number of arguments differs
            checks.add(new Check(Base.class, "plain", void.class, Arrays.copyOf(repeated, count)));
        }

        // "Aa" and "BB" have the same hash code, so all of these names do.
        assertEquals("AaAaAa".hashCode(), "BBBBBB".hashCode());

        for (String first : List.of("Aa", "BB")) {
            for (String second : List.of("Aa", "BB")) {
                for (String third : List.of("Aa", "BB")) {
                    checks.add(new Check(Base.class, first + second + third, null, int.class));
                    checks.add(new Check(Base.class, first + second + third, void.class, int.class));
                }
            }
        }

        // with and without a return type, and with the return type where an argument could be
        checks.add(new Check(Base.class, "plain", null, int.class, String.class));
        checks.add(new Check(Base.class, "plain", String.class, int.class));
        checks.add(new Check(Base.class, "plain", int.class, String.class));

        for (Check check : checks) {
            check.viaHelper(Direct.class);
        }

        Method find = NytheriaCompatHelper.MethodOverrideCache.class.getDeclaredMethod("findOverrideClassValue", Class.class, String.class, Class[].class, Class.class, boolean.class);
        find.setAccessible(true);
        Set<Object> classValues = new HashSet<>();

        for (Check check : checks) {
            Object found = find.invoke(NytheriaCompatHelper.MethodOverrideCache.INSTANCE, check.superClass, check.methodName, check.methodArgs.clone(), check.returnType, false);
            Object ofMap = NytheriaCompatHelper.MethodOverrideCache.INSTANCE.getOverrideClassValue(new NytheriaCompatHelper.OverrideData(check.superClass, check.methodName, check.methodArgs.clone(), check.returnType));

            assertSame(ofMap, found, () -> "the lookup table has another check for " + check);
            assertTrue(classValues.add(found), () -> "two checks share a ClassValue: " + check);
            assertEquals(check.reference(Direct.class), check.viaHelper(Direct.class), () -> "helper " + check);
            assertEquals(check.reference(Covariant.class), check.prepared().test(Covariant.class), () -> "prepared " + check);
        }

        assertTrue(sharedSlots(checks) > 100, "too few checks of this test share a slot of the lookup table");
        assertLookupTableIsConsistent();
    }

    /** How many of these checks are in a slot of the lookup table that holds another of them as well. */
    private static int sharedSlots(List<Check> checks) throws ReflectiveOperationException {
        Field tableField = NytheriaCompatHelper.MethodOverrideCache.class.getDeclaredField("overrideLookupTable");
        tableField.setAccessible(true);
        Set<Object> keys = new HashSet<>();

        for (Check check : checks) {
            keys.add(new NytheriaCompatHelper.OverrideData(check.superClass, check.methodName, check.methodArgs, check.returnType));
        }

        int shared = 0;

        for (Object head : (Object[]) tableField.get(NytheriaCompatHelper.MethodOverrideCache.INSTANCE)) {
            int inSlot = 0;

            for (Object entry = head; entry != null; entry = field(entry, "next")) {
                if (keys.contains(field(entry, "overrideData"))) {
                    inSlot++;
                }
            }

            if (inSlot > 1) {
                shared += inSlot;
            }
        }

        return shared;
    }

    @Test
    public void classesLoadedLaterAreLookedUpForThemselves() throws Exception {
        Check plain = new Check(Base.class, "plain", null, int.class);
        Check typed = new Check(Base.class, "returned", String.class);
        NytheriaCompatHelper.MethodOverrideCheck preparedPlain = plain.prepared();
        NytheriaCompatHelper.MethodOverrideCheck preparedTyped = typed.prepared();

        // Used before the other classes exist.
        assertTrue(plain.viaHelper(Direct.class));
        assertTrue(preparedTyped.test(Covariant.class));

        for (int i = 0; i < 4; i++) {
            LaterLoader loader = new LaterLoader(MethodOverrideCheckTest.class.getClassLoader());

            for (Class<?> original : List.of(Direct.class, Untouched.class, Covariant.class, Leaf.class)) {
                Class<?> later = loader.loadClass(original.getName());

                assertNotSame(original, later);
                assertSame(loader, later.getClassLoader());

                for (Check check : List.of(plain, typed)) {
                    assertEquals(check.reference(original), check.reference(later));
                    assertEquals(check.reference(later), check.viaHelper(later), () -> check + " for later " + later);
                }

                assertEquals(plain.reference(later), preparedPlain.test(later));
                assertEquals(typed.reference(later), preparedTyped.test(later));
            }
        }
    }

    @Test
    public void checkedClassesCanBeUnloaded() throws Exception {
        Check plain = new Check(Base.class, "plain", null, int.class);
        NytheriaCompatHelper.MethodOverrideCheck prepared = new Check(Base.class, "returned", Object.class).prepared();
        LaterLoader loader = new LaterLoader(MethodOverrideCheckTest.class.getClassLoader());
        Class<?> later = loader.loadClass(Covariant.class.getName());

        assertFalse(plain.viaHelper(later));
        assertTrue(prepared.test(later));

        WeakReference<Class<?>> classReference = new WeakReference<>(later);
        WeakReference<ClassLoader> loaderReference = new WeakReference<>(loader);
        later = null;
        loader = null;

        for (int i = 0; i < 100 && (classReference.get() != null || loaderReference.get() != null); i++) {
            System.gc();
            Thread.sleep(50);
        }

        assertSame(null, classReference.get(), "the helper keeps a checked class alive");
        assertSame(null, loaderReference.get(), "the helper keeps the class loader of a checked class alive");
        // The checks themselves still work.
        assertTrue(prepared.test(Covariant.class));
        assertTrue(plain.viaHelper(Direct.class));
    }

    @Test
    public void concurrentFirstUse() throws Exception {
        int threads = 16;
        int rounds = 40;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            // Array types are a cheap supply of classes nobody has asked about before: with them as the "superclass"
            // every round gets checks that are new to the helper, each with its own ClassValue, both answers included.
            Class<?> fresh = MethodOverrideCheckTest.class;

            for (int round = 0; round < rounds; round++) {
                List<Class<?>> superClasses = new ArrayList<>();

                for (int i = 0; i < 2; i++) {
                    fresh = fresh.arrayType();
                    superClasses.add(fresh);
                }

                superClasses.add(Base.class);
                List<Check> checks = checksFor(superClasses);
                // Checks that only this round uses and that are overridden for some of the classes.
                String roundName = "round" + round;
                checks.add(new Check(Base.class, roundName, null));
                checks.add(new Check(Extension.class, roundName, int.class, String.class));

                List<NytheriaCompatHelper.MethodOverrideCheck> prepared = new ArrayList<>();

                for (Check check : checks) {
                    prepared.add(check.prepared());
                }

                boolean[][] expected = new boolean[checks.size()][TOP_CLASSES.size()];

                for (int c = 0; c < checks.size(); c++) {
                    for (int t = 0; t < TOP_CLASSES.size(); t++) {
                        expected[c][t] = checks.get(c).reference(TOP_CLASSES.get(t));
                    }
                }

                CyclicBarrier start = new CyclicBarrier(threads);
                List<Future<String>> results = new ArrayList<>();

                for (int thread = 0; thread < threads; thread++) {
                    int threadIndex = thread;

                    results.add(executor.submit(() -> {
                        start.await(30, TimeUnit.SECONDS);

                        // Every thread walks the checks from another starting point, half of them through the
                        // prepared check first.
                        for (int n = 0; n < checks.size(); n++) {
                            int c = (n + threadIndex * 7) % checks.size();

                            for (int t = 0; t < TOP_CLASSES.size(); t++) {
                                Class<?> topClass = TOP_CLASSES.get(t);
                                boolean viaPrepared = threadIndex % 2 == 0 ? prepared.get(c).test(topClass) : checks.get(c).viaHelper(topClass);
                                boolean viaOther = threadIndex % 2 == 0 ? checks.get(c).viaHelper(topClass) : prepared.get(c).test(topClass);

                                if (viaPrepared != expected[c][t] || viaOther != expected[c][t]) {
                                    return checks.get(c) + " for " + topClass + ": expected " + expected[c][t] + ", got " + viaPrepared + " and " + viaOther;
                                }
                            }
                        }

                        return null;
                    }));
                }

                for (Future<String> result : results) {
                    assertSame(null, result.get(60, TimeUnit.SECONDS));
                }
            }
        } finally {
            executor.shutdownNow();
        }

        assertLookupTableIsConsistent();
    }

    @Test
    public void lookupsDoNotBuildKeys() {
        com.sun.management.ThreadMXBean threadBean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long threadId = Thread.currentThread().threadId();
        NytheriaCompatHelper.MethodOverrideCheck prepared = NytheriaCompatHelper.MethodOverrideCheck.of(Base.class, "plain", int.class, String.class);
        int calls = 200_000;
        int hits = 0;

        // Known to the helper before anything is measured.
        for (int i = 0; i < 1000; i++) {
            hits += prepared.test(Leaf.class) ? 1 : 0;
            hits += hasMethodOverride(Leaf.class, Base.class, "plain", int.class, String.class) ? 1 : 0;
            hits += hasMethodOverrideWithReturnType(Leaf.class, Base.class, "plain", void.class, int.class, String.class) ? 1 : 0;
        }

        long before = threadBean.getThreadAllocatedBytes(threadId);

        for (int i = 0; i < calls; i++) {
            hits += prepared.test(Leaf.class) ? 1 : 0;
        }

        long preparedBytes = threadBean.getThreadAllocatedBytes(threadId) - before;
        before = threadBean.getThreadAllocatedBytes(threadId);

        for (int i = 0; i < calls; i++) {
            hits += hasMethodOverride(Leaf.class, Base.class, "plain", int.class, String.class) ? 1 : 0;
        }

        long helperBytes = threadBean.getThreadAllocatedBytes(threadId) - before;
        before = threadBean.getThreadAllocatedBytes(threadId);

        for (int i = 0; i < calls; i++) {
            hits += hasMethodOverrideWithReturnType(Leaf.class, Base.class, "plain", void.class, int.class, String.class) ? 1 : 0;
        }

        long typedBytes = threadBean.getThreadAllocatedBytes(threadId) - before;

        assertEquals(3 * (1000 + calls), hits);
        // A prepared check creates nothing at all.
        assertTrue(preparedBytes < calls, "prepared check: " + preparedBytes + " bytes for " + calls + " calls");
        // The helper is left with at most the Class[2] of the varargs call (24 bytes, 32 without compressed pointers);
        // a key object next to it would be another 32 (48) bytes.
        assertTrue(helperBytes <= 40L * calls, "hasMethodOverride: " + helperBytes + " bytes for " + calls + " calls");
        assertTrue(typedBytes <= 40L * calls, "hasMethodOverrideWithReturnType: " + typedBytes + " bytes for " + calls + " calls");
    }

    // ---- support --------------------------------------------------------------------------------------------------

    /**
     * The lookup table of the helper may only repeat what its map of checks holds: every entry has to be a key of the
     * map with the very same ClassValue, and no key may be in the table twice.
     */
    private static void assertLookupTableIsConsistent() {
        try {
            Field tableField = NytheriaCompatHelper.MethodOverrideCache.class.getDeclaredField("overrideLookupTable");
            Field sizeField = NytheriaCompatHelper.MethodOverrideCache.class.getDeclaredField("overrideLookupSize");
            Field mapField = NytheriaCompatHelper.MethodOverrideCache.class.getDeclaredField("cachedClassValues");
            tableField.setAccessible(true);
            sizeField.setAccessible(true);
            mapField.setAccessible(true);

            Object[] table = (Object[]) tableField.get(NytheriaCompatHelper.MethodOverrideCache.INSTANCE);
            int size = (int) sizeField.get(NytheriaCompatHelper.MethodOverrideCache.INSTANCE);
            Map<?, ?> map = (Map<?, ?>) mapField.get(NytheriaCompatHelper.MethodOverrideCache.INSTANCE);
            Set<Object> keys = new HashSet<>();
            int entries = 0;

            assertEquals(1, Integer.bitCount(table.length));

            for (Object head : table) {
                for (Object entry = head; entry != null; entry = field(entry, "next")) {
                    Object key = field(entry, "overrideData");

                    assertTrue(keys.add(key), () -> "twice in the lookup table: " + key);
                    assertSame(map.get(key), field(entry, "classValue"), () -> "not the ClassValue of the map: " + key);
                    entries++;
                }
            }

            assertEquals(size, entries);
            assertTrue(entries * 2 <= table.length, entries + " entries in a table of " + table.length);
            assertTrue(entries > 64, "the table never grew: " + entries + " entries");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    /** Defines the fixture classes that override something a second time; everything else comes from the parent. */
    private static final class LaterLoader extends ClassLoader {
        private static final Set<String> OWN = Set.of(Direct.class.getName(), Untouched.class.getName(), Covariant.class.getName(), Leaf.class.getName());

        LaterLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!OWN.contains(name)) {
                return super.loadClass(name, resolve);
            }

            synchronized (this.getClassLoadingLock(name)) {
                Class<?> loaded = this.findLoadedClass(name);

                if (loaded == null) {
                    try (InputStream stream = this.getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (stream == null) {
                            throw new ClassNotFoundException(name);
                        }

                        byte[] bytes = stream.readAllBytes();
                        loaded = this.defineClass(name, bytes, 0, bytes.length);
                    } catch (java.io.IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }

                return loaded;
            }
        }
    }
}
