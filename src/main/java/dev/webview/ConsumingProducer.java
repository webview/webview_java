package dev.webview;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;

public interface ConsumingProducer<C, P> {

    public @Nullable P produce(@Nullable C consume) throws InterruptedException;

    public static <C, P> ConsumingProducer<C, P> of(@NonNull Class<C> consumingClazz, @Nullable P result) {
        return (aVoid) -> {
            return result;
        };
    }

}
