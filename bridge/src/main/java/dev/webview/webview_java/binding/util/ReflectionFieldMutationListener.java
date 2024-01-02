package dev.webview.webview_java.binding.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.async.AsyncTask;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * This basically works by checking the hashCode of the value in a field and
 * comparing it against a known value.
 */
public class ReflectionFieldMutationListener {
    private static final int POLL_INTERVAL = 500;
    private static final int NULL_V = 0;

    private Field field;
    private WeakReference<Object> $inst;
    private AsyncTask task;
    private int lastHash;

    private @Nullable Consumer<@Nullable Object> onMutate;

    public ReflectionFieldMutationListener(@NonNull Field field, @Nullable Object instance) {
        this(field, instance != null ? new WeakReference<>(instance) : null);
    }

    public ReflectionFieldMutationListener(@NonNull Field field, @Nullable WeakReference<Object> instance) {
        ReflectionAccessHelper.makeAccessible(field);

        this.field = field;
        this.$inst = instance;
        this.lastHash = 0;

        this.task = AsyncTask.create(this::asyncChecker);
    }

    @SuppressWarnings("unchecked")
    public <T> void onMutate(@Nullable final Consumer<@Nullable T> consumer) {
        this.onMutate = (Consumer<Object>) consumer;
    }

    @SneakyThrows
    private void asyncChecker() {
        while (true) {
            Object instance = null;

            if ($inst != null) {
                instance = $inst.get();

                if (instance == null) {
                    return; // We lost the reference, close the handler entirely.
                }
            }

            try {
                int currentHash = getHashCodeForField(this.field, instance);

                if (this.lastHash != currentHash) {
                    this.lastHash = currentHash;

                    if (this.onMutate != null) {
                        this.onMutate.accept(
                            this.field.get(instance)
                        );
                    }
                }
            } catch (Throwable t) {
                // We ignore any exception thrown.
            }

            Thread.yield();
            Thread.sleep(POLL_INTERVAL);
        }
    }

    public void stopWatching() {
        this.task.cancel();
    }

    private static int getHashCodeForField(Field field, Object instance) throws IllegalArgumentException, IllegalAccessException {
        Object v = field.get(instance);

        if (v == null) {
            return NULL_V;
        } else {
            return v.hashCode();
        }
    }

}
