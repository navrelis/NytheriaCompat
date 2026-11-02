package dev.nytheria.compat.hook;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Cost of {@link EventGate#has} against {@code NeoForge.EVENT_BUS.post}. Not a test: run by {@code testing/eventgate/run.sh},
 * one JVM per row, so that one row's profile cannot change how the next is compiled. No JMH: it would be a new dependency.
 *
 * <p>{@code java EventGateBench hot <row> <warmup s> <measure s>} prints {@code row ns/op} per measured second.
 * {@code java EventGateBench cold <row> <samples>} prints percentiles of one first call per sample, each on a new class.
 */
public final class EventGateBench {
    private static final Class<? extends Event> NONE = LivingBreatheEvent.class;
    private static final Class<? extends Event> ONE = EntityTickEvent.Post.class;
    private static final Class<?>[] MIXED = {LivingBreatheEvent.class, LivingDrownEvent.class, LivingEquipmentChangeEvent.class, LivingFallEvent.class,
            MobEffectEvent.Added.class, MobEffectEvent.Applicable.class, GetEnchantmentLevelEvent.class, EntityEvent.Size.class};
    private static final int BATCH = 1 << 14;
    // about as many listener lists as the pack has; the bus looks each post up in that map
    private static final int LISTS = 300;

    // everything a row computes ends up here, so that the work cannot be removed
    static long sink;
    static Event escaped;

    private interface Row {
        void batch();
    }

    public static class Cold extends Event {
    }

    /** A new class per instance, so that neither the bus nor the gate has seen it. */
    private static final class Fresh extends ClassLoader {
        private static final byte[] BYTES;

        static {
            try (InputStream in = Cold.class.getClassLoader().getResourceAsStream(Cold.class.getName().replace('.', '/') + ".class")) {
                BYTES = in.readAllBytes();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private Fresh() {
            super(Cold.class.getClassLoader());
        }

        @SuppressWarnings("unchecked")
        static Class<? extends Event> next() {
            return (Class<? extends Event>) new Fresh().defineClass(Cold.class.getName(), BYTES, 0, BYTES.length);
        }
    }

    public static void main(String[] args) throws Exception {
        NeoForge.EVENT_BUS.addListener(EntityTickEvent.Post.class, event -> sink++);
        NeoForge.EVENT_BUS.start();
        if (!args[1].equals("very-first")) for (int i = 0; i < LISTS; i++) EventGate.has(Fresh.next());
        if (args[0].equals("hot")) hot(args[1], row(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        else cold(args[1], Integer.parseInt(args[2]));
    }

    // ---- hot rows: one batch per method, so that each is compiled on its own -----------------------------------------

    private static Row row(String name) {
        return switch (name) {
            case "empty" -> EventGateBench::empty;
            case "has-none" -> EventGateBench::hasNone;
            case "has-none-mixed" -> EventGateBench::hasNoneMixed;
            case "has-one" -> EventGateBench::hasOne;
            case "new-escape" -> EventGateBench::newEscape;
            case "post-none-prebuilt" -> EventGateBench::postNonePrebuilt;
            case "post-none-new" -> EventGateBench::postNoneNew;
            case "post-none-new-escape" -> EventGateBench::postNoneNewEscape;
            case "post-one-prebuilt" -> EventGateBench::postOnePrebuilt;
            case "gated-one-prebuilt" -> EventGateBench::gatedOnePrebuilt;
            default -> throw new IllegalArgumentException(name);
        };
    }

    private static void empty() {
        long s = 0;
        for (int i = 0; i < BATCH; i++) s += i;
        sink += s;
    }

    @SuppressWarnings("unchecked")
    private static void hasNoneMixed() {
        long s = 0;
        for (int i = 0; i < BATCH; i++) if (EventGate.has((Class<? extends Event>) MIXED[i & 7])) s++;
        sink += s;
    }

    private static void hasNone() {
        long s = 0;
        for (int i = 0; i < BATCH; i++) if (EventGate.has(LivingBreatheEvent.class)) s++;
        sink += s;
    }

    private static void hasOne() {
        long s = 0;
        for (int i = 0; i < BATCH; i++) if (EventGate.has(EntityTickEvent.Post.class)) s++;
        sink += s;
    }

    private static void newEscape() {
        for (int i = 0; i < BATCH; i++) escaped = new LivingBreatheEvent(null, true, i, i);
    }

    private static final LivingBreatheEvent NONE_EVENT = new LivingBreatheEvent(null, true, 1, 1);
    private static final EntityTickEvent.Post ONE_EVENT = new EntityTickEvent.Post(null);

    private static void postNonePrebuilt() {
        long s = 0;
        for (int i = 0; i < BATCH; i++) s += NeoForge.EVENT_BUS.post(NONE_EVENT).getConsumeAirAmount();
        sink += s;
    }

    // the event stays local, so the JIT may remove it if it sees all of post: the best case for post
    private static void postNoneNew() {
        long s = 0;
        for (int i = 0; i < BATCH; i++) s += NeoForge.EVENT_BUS.post(new LivingBreatheEvent(null, true, i, i)).getConsumeAirAmount();
        sink += s;
    }

    // the event has to exist: the worst case for post
    private static void postNoneNewEscape() {
        for (int i = 0; i < BATCH; i++) escaped = NeoForge.EVENT_BUS.post(new LivingBreatheEvent(null, true, i, i));
    }

    private static void postOnePrebuilt() {
        for (int i = 0; i < BATCH; i++) NeoForge.EVENT_BUS.post(ONE_EVENT);
    }

    private static void gatedOnePrebuilt() {
        for (int i = 0; i < BATCH; i++) if (EventGate.has(EntityTickEvent.Post.class)) NeoForge.EVENT_BUS.post(ONE_EVENT);
    }

    private static void hot(String name, Row row, int warmup, int measure) {
        System.out.println("# warmup " + warmup + " s");
        run(row, warmup * 1_000_000_000L);
        for (int i = 0; i < measure; i++) System.out.printf(Locale.ROOT, "%s\t%.3f%n", name, run(row, 1_000_000_000L));
        if (sink == 42) System.out.println(escaped);
    }

    private static double run(Row row, long nanos) {
        long start = System.nanoTime(), end = start + nanos, batches = 0, now;
        do {
            row.batch();
            batches++;
        } while ((now = System.nanoTime()) < end);
        return (now - start) / (double) (batches * BATCH);
    }

    // ---- cold rows: a first call, timed alone ---------------------------------------------------------------------

    private static void cold(String name, int samples) throws Exception {
        long[] ns = new long[samples];
        for (int i = 0; i < samples; i++) ns[i] = switch (name) {
            // the first call of this JVM: EventGate's class init and its first reflective call come on top
            case "very-first" -> firstHas(Fresh.next());
            // the list of the class does not exist yet
            case "first-has" -> firstHas(Fresh.next());
            case "first-post" -> firstPost(Fresh.next());
            // the list exists, the gate has not asked for it
            case "first-has-after-post" -> {
                Class<? extends Event> type = Fresh.next();
                firstPost(type);
                yield firstHas(type);
            }
            // a listener came in: rebuild[i listeners], paid by whoever reads the list first
            case "rebuild-has", "rebuild-post" -> {
                Class<? extends Event> type = Fresh.next();
                EventGate.has(type);
                for (int k = 0; k < 1 + i % 8; k++) NeoForge.EVENT_BUS.addListener(type, event -> sink++);
                yield name.equals("rebuild-has") ? firstHas(type) : firstPost(type);
            }
            default -> throw new IllegalArgumentException(name);
        };
        long first = ns[0];
        Arrays.sort(ns);
        System.out.printf(Locale.ROOT, "%s\tfirst %d\tp50 %d\tp90 %d\tp99 %d%n", name, first, ns[samples / 2], ns[samples * 9 / 10], ns[samples * 99 / 100]);
    }

    private static long firstHas(Class<? extends Event> type) {
        long start = System.nanoTime();
        boolean has = EventGate.has(type);
        long end = System.nanoTime();
        if (has) sink++;
        return end - start;
    }

    private static long firstPost(Class<? extends Event> type) throws Exception {
        Event event = type.getDeclaredConstructor().newInstance();
        long start = System.nanoTime();
        NeoForge.EVENT_BUS.post(event);
        return System.nanoTime() - start;
    }
}
