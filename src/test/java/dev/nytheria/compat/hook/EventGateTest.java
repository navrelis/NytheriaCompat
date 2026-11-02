package dev.nytheria.compat.hook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.Test;

/**
 * {@link EventGate#has} has to say whether {@code NeoForge.EVENT_BUS.post} would reach a listener, at any time. A class is
 * used in one test only: the bus is shared, and nothing can take a listener off it again.
 */
public class EventGateTest {
    // ---- fixtures -------------------------------------------------------------------------------------------------

    public static class NoListener extends Event {
    }

    public static class Exact extends Event {
    }

    public static class Parent extends Event {
    }

    public static class Child extends Parent {
    }

    public static class Unrelated extends Event {
    }

    public static class Base extends Event {
    }

    public static class Derived extends Base {
    }

    public static class Early extends Event {
    }

    public static class Late extends Event {
    }

    public static class Registered {
        @net.neoforged.bus.api.SubscribeEvent
        public void onEvent(Registers event) {
        }
    }

    public static class Registers extends Event {
    }

    public static class Removed extends Event {
    }

    public static class Unhooked {
        @net.neoforged.bus.api.SubscribeEvent
        public void onEvent(Removed event) {
        }
    }

    public static class Allocations extends Event {
    }

    // ---- tests ----------------------------------------------------------------------------------------------------

    @Test
    public void falseWithoutListener() {
        assertFalse(EventGate.has(NoListener.class));
    }

    @Test
    public void trueForTheExactClass() {
        NeoForge.EVENT_BUS.addListener(Exact.class, event -> {
        });

        assertTrue(EventGate.has(Exact.class));
    }

    @Test
    public void listenerOfSuperclassCounts() {
        NeoForge.EVENT_BUS.addListener(Parent.class, event -> {
        });

        assertTrue(EventGate.has(Parent.class));
        assertTrue(EventGate.has(Child.class));
        assertFalse(EventGate.has(Unrelated.class));
    }

    @Test
    public void listenerOfSubclassDoesNotCount() {
        NeoForge.EVENT_BUS.addListener(Derived.class, event -> {
        });

        assertTrue(EventGate.has(Derived.class));
        assertFalse(EventGate.has(Base.class));
    }

    @Test
    public void listenerAddedAfterTheFirstCallCounts() {
        assertFalse(EventGate.has(Late.class));

        NeoForge.EVENT_BUS.addListener(Late.class, event -> {
        });

        assertTrue(EventGate.has(Late.class));
    }

    @Test
    public void listenerObjectCounts() {
        NeoForge.EVENT_BUS.register(new Registered());

        assertTrue(EventGate.has(Registers.class));
    }

    @Test
    public void unregisteredListenerStopsCounting() {
        Unhooked listener = new Unhooked();
        NeoForge.EVENT_BUS.register(listener);
        assertTrue(EventGate.has(Removed.class));

        NeoForge.EVENT_BUS.unregister(listener);

        assertFalse(EventGate.has(Removed.class));
    }

    /** The bus starts shut down: {@code has} does not wait for that, and a post after the gate reaches the listener. */
    @Test
    public void whatTheGateLetsThroughReachesTheListener() {
        AtomicInteger calls = new AtomicInteger();
        Consumer<Early> listener = event -> calls.incrementAndGet();
        NeoForge.EVENT_BUS.addListener(Early.class, listener);
        NeoForge.EVENT_BUS.start();

        if (EventGate.has(Early.class)) NeoForge.EVENT_BUS.post(new Early());

        assertTrue(calls.get() == 1);
    }

    @Test
    public void doesNotAllocateAfterTheFirstCall() {
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().threadId();
        EventGate.has(Allocations.class);

        long before = threads.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 100_000; i++) EventGate.has(Allocations.class);
        long allocated = threads.getThreadAllocatedBytes(thread) - before;

        // the measuring call itself may allocate a little; one object per call would be megabytes
        assertTrue(allocated < 100_000, allocated + " bytes");
    }
}
