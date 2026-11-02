package dev.nytheria.compat.hook;

import net.neoforged.bus.EventBus;
import net.neoforged.bus.ListenerList;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Method;

/**
 * The gate in front of {@code NeoForge.EVENT_BUS.post}: a hook builds and posts an event only if a listener would get it.
 *
 * <pre>{@code
 * if (EventGate.has(LivingFallEvent.class)) NeoForge.EVENT_BUS.post(new LivingFallEvent(...));
 * }</pre>
 *
 * <p>Pass the exact class that is posted: a listener of a superclass counts, a listener of a subclass does not, like on
 * the bus. {@link #has} reads the same listener list {@code post} reads, so it is never out of date: a listener registered
 * at any time, before or after {@code FMLCommonSetupEvent}, counts from then on, and one that is unregistered stops
 * counting. No event can be lost to the gate, and it needs no set-up. The list of a class is looked up once; after that a
 * call allocates nothing.
 */
public final class EventGate {
    // EventBus keeps this lookup private and has no public way to ask for listeners. The method is the same in bus 7.x and 8.x.
    private static final Method LISTENER_LIST;

    static {
        try {
            LISTENER_LIST = EventBus.class.getDeclaredMethod("getListenerList", Class.class);
            LISTENER_LIST.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final ClassValue<ListenerList> LISTS = new ClassValue<>() {
        @Override
        protected ListenerList computeValue(Class<?> type) {
            try {
                return (ListenerList) LISTENER_LIST.invoke(NeoForge.EVENT_BUS, type);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    private EventGate() {
    }

    public static boolean has(Class<? extends Event> type) {
        return LISTS.get(type).getListeners().length != 0;
    }
}
