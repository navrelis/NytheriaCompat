package dev.nytheria.compat.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Nytheria Compat: remembers the objects that resolved a tag-dependent result to "nothing" before the tags were bound for the
 * first time in this game run, and makes them forget that result once the tags are bound, so they resolve it again.
 *
 * <p>Until the first data pack load has finished, every tag is empty. A result that is computed once and then kept
 * (the items of an ingredient) is therefore wrong for good when somebody asks for it that early. From the first
 * binding on nothing is remembered any more: a later reload is evaluated against the tags of the previous one, as in
 * vanilla, and an object that is still empty then really has no items.
 *
 * <p>The objects are held weakly and the list is dropped at the first binding. {@link #resolvedEmpty} may be called
 * from any thread; it is meant for the place where the result is computed, not for a path that only reads it.
 */
public final class EmptyBeforeTags<T> {
    private final Consumer<? super T> forget;
    private volatile boolean tagsBound;
    // Guarded by this; null once the tags are bound.
    private List<WeakReference<T>> resolvedEmpty = new ArrayList<>();

    /**
     * @param forget makes an object drop its kept result, so that the next request computes it again
     */
    public EmptyBeforeTags(Consumer<? super T> forget) {
        this.forget = forget;
    }

    /** To be called when {@code owner} has just computed an empty result and is about to keep it. */
    public void resolvedEmpty(T owner) {
        if (this.tagsBound) {
            return;
        }

        synchronized (this) {
            if (this.resolvedEmpty != null) {
                this.resolvedEmpty.add(new WeakReference<>(owner));
            }
        }
    }

    /** To be called every time tags have been bound; only the first call does anything. */
    public void tagsBound() {
        if (this.tagsBound) {
            return;
        }

        List<WeakReference<T>> early;

        synchronized (this) {
            early = this.resolvedEmpty;
            this.resolvedEmpty = null;
            this.tagsBound = true;
        }

        if (early == null) {
            return;
        }

        for (WeakReference<T> reference : early) {
            T owner = reference.get();

            if (owner != null) {
                this.forget.accept(owner);
            }
        }
    }

    public boolean areTagsBound() {
        return this.tagsBound;
    }
}
