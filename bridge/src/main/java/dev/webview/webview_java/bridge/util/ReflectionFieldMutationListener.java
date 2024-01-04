/**
 * MIT LICENSE
 *
 * Copyright (c) 2024 Alex Bowles @ Casterlabs
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.webview.webview_java.bridge.util;

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
    private static final int POLL_INTERVAL = 25;
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
