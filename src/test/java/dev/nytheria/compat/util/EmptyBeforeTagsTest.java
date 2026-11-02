package dev.nytheria.compat.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * {@link EmptyBeforeTags} with stand-ins for what it is used for: {@link Items} keeps what it resolved on the first
 * request, like an ingredient keeps its item stacks, and {@link Tags} has nothing in it until it is bound, like the
 * item registry before the first data pack load has finished. Plain unit tests, no game classes.
 */
public class EmptyBeforeTagsTest {
    /** The tags of the game: empty until bound. */
    private static final class Tags {
        private final Map<String, List<String>> bound = new HashMap<>();

        List<String> get(String tag) {
            return this.bound.getOrDefault(tag, List.of());
        }
    }

    /** Resolves its items once and keeps them, as {@code Ingredient#getItems} does, and reports an empty result. */
    private static class Items {
        final Tags tags;
        final String tag;
        final EmptyBeforeTags<Items> tracker;
        List<String> kept;
        int resolved;
        int forgotten;

        Items(Tags tags, String tag, EmptyBeforeTags<Items> tracker) {
            this.tags = tags;
            this.tag = tag;
            this.tracker = tracker;
        }

        List<String> resolve() {
            return this.tags.get(this.tag);
        }

        final List<String> get() {
            if (this.kept == null) {
                this.kept = this.resolve();
                this.resolved++;

                if (this.kept.isEmpty()) {
                    this.tracker.resolvedEmpty(this);
                }
            }

            return this.kept;
        }

        void forget() {
            this.kept = null;
            this.forgotten++;
        }
    }

    /** Like neoforge:difference: its items are those of {@code base} without those of {@code subtracted}. */
    private static final class Difference extends Items {
        final Items base;
        final Items subtracted;

        Difference(Tags tags, EmptyBeforeTags<Items> tracker, Items base, Items subtracted) {
            super(tags, null, tracker);
            this.base = base;
            this.subtracted = subtracted;
        }

        @Override
        List<String> resolve() {
            List<String> items = new ArrayList<>(this.base.get());
            items.removeIf(item -> this.subtracted.get().contains(item));
            return items;
        }
    }

    private static EmptyBeforeTags<Items> tracker() {
        return new EmptyBeforeTags<>(Items::forget);
    }

    @Test
    public void askedBeforeTheTagsAreBoundResolvesAgainAfterwards() {
        Tags tags = new Tags();
        EmptyBeforeTags<Items> tracker = tracker();
        Items wooden = new Items(tags, "c:chests/wooden", tracker);
        Items trapped = new Items(tags, "c:chests/trapped", tracker);
        Difference chest = new Difference(tags, tracker, wooden, trapped);

        // Another mod looks at the recipe while the recipes are loaded, and cleans up after itself, but it only knows
        // about the ingredient it asked, not about the two inside it.
        assertEquals(List.of(), chest.get());
        chest.kept = null;
        assertEquals(List.of(), wooden.kept);

        // The data pack load finishes.
        tags.bound.put("c:chests/wooden", List.of("chest", "trapped_chest"));
        tags.bound.put("c:chests/trapped", List.of("trapped_chest"));
        assertFalse(tracker.areTagsBound());
        tracker.tagsBound();
        assertTrue(tracker.areTagsBound());

        assertEquals(List.of("chest"), chest.get());
        assertEquals(List.of("chest", "trapped_chest"), wooden.get());
        assertEquals(1, wooden.forgotten);
        // And it is kept from now on.
        assertSame(chest.get(), chest.get());
        assertEquals(2, wooden.resolved);
    }

    @Test
    public void withoutTheRetryTheEmptyResultStays() {
        // What happened before: nobody tells the inner ingredient to resolve again.
        Tags tags = new Tags();
        EmptyBeforeTags<Items> tracker = tracker();
        Items wooden = new Items(tags, "c:chests/wooden", tracker);
        Difference chest = new Difference(tags, tracker, wooden, new Items(tags, "c:chests/trapped", tracker));

        chest.get();
        chest.kept = null;
        tags.bound.put("c:chests/wooden", List.of("chest"));

        assertEquals(List.of(), chest.get());
    }

    @Test
    public void nothingChangesForResultsThatAreNotEmpty() {
        Tags tags = new Tags();
        tags.bound.put("planks", List.of("oak_planks"));
        EmptyBeforeTags<Items> tracker = tracker();
        Items planks = new Items(tags, "planks", tracker);

        List<String> before = planks.get();
        tracker.tagsBound();

        assertSame(before, planks.get());
        assertEquals(1, planks.resolved);
        assertEquals(0, planks.forgotten);
    }

    @Test
    public void emptyAfterTheTagsAreBoundIsKept() {
        Tags tags = new Tags();
        EmptyBeforeTags<Items> tracker = tracker();
        tracker.tagsBound();
        Items nothing = new Items(tags, "c:ingots/of_a_mod_that_is_not_installed", tracker);

        for (int i = 0; i < 100; i++) {
            assertEquals(List.of(), nothing.get());
        }

        // Later bindings (a reload, another world) do not touch it either.
        tracker.tagsBound();
        tracker.tagsBound();
        nothing.get();

        assertEquals(1, nothing.resolved);
        assertEquals(0, nothing.forgotten);
    }

    @Test
    public void onlyTheFirstBindingMakesThemForget() {
        Tags tags = new Tags();
        EmptyBeforeTags<Items> tracker = tracker();
        Items early = new Items(tags, "tag", tracker);
        early.get();

        tracker.tagsBound();
        tracker.tagsBound();
        tracker.tagsBound();

        assertEquals(1, early.forgotten);
    }

    @Test
    public void rememberedObjectsCanBeCollected() throws Exception {
        Tags tags = new Tags();
        AtomicInteger forgotten = new AtomicInteger();
        EmptyBeforeTags<Items> tracker = new EmptyBeforeTags<>(items -> forgotten.incrementAndGet());
        Items early = new Items(tags, "tag", tracker);
        early.get();
        WeakReference<Items> reference = new WeakReference<>(early);
        early = null;

        for (int i = 0; i < 100 && reference.get() != null; i++) {
            System.gc();
            Thread.sleep(50);
        }

        assertSame(null, reference.get(), "the tracker keeps a remembered object alive");
        tracker.tagsBound();
        assertEquals(0, forgotten.get());
    }

    @Test
    public void rememberingFromManyThreads() throws Exception {
        int threads = 16;
        int perThread = 2000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            for (int round = 0; round < 20; round++) {
                Tags tags = new Tags();
                EmptyBeforeTags<Items> tracker = tracker();
                boolean bindWhileRemembering = round % 2 == 1;
                CyclicBarrier start = new CyclicBarrier(threads);
                List<Future<List<Items>>> results = new ArrayList<>();

                for (int thread = 0; thread < threads; thread++) {
                    int threadIndex = thread;

                    results.add(executor.submit(() -> {
                        List<Items> mine = new ArrayList<>();
                        start.await(30, TimeUnit.SECONDS);

                        for (int i = 0; i < perThread; i++) {
                            Items items = new Items(tags, "tag", tracker);
                            items.get();
                            mine.add(items);

                            if (bindWhileRemembering && threadIndex == 0 && i == perThread / 2) {
                                tracker.tagsBound();
                            }
                        }

                        return mine;
                    }));
                }

                List<Items> all = new ArrayList<>();

                for (Future<List<Items>> result : results) {
                    all.addAll(result.get(60, TimeUnit.SECONDS));
                }

                tracker.tagsBound();
                int forgotten = 0;

                for (Items items : all) {
                    assertTrue(items.forgotten <= 1, "forgotten " + items.forgotten + " times");
                    forgotten += items.forgotten;
                }

                if (bindWhileRemembering) {
                    // Everything the binding thread itself resolved before it bound the tags was forgotten.
                    assertTrue(forgotten >= perThread / 2, "only " + forgotten + " forgotten");
                    assertTrue(forgotten < threads * perThread);
                } else {
                    assertEquals(threads * perThread, forgotten);
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
