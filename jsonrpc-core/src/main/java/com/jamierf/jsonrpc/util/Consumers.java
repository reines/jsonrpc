package com.jamierf.jsonrpc.util;

import com.google.common.base.Throwables;

import java.util.function.Consumer;

public final class Consumers {

    @FunctionalInterface
    public interface PropagatingConsumer<T> {
        void accept(final T t) throws Exception;
    }

    public static <T> Consumer<T> propagate(final PropagatingConsumer<T> delegate) {
        return t -> {
            try {
                delegate.accept(t);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        };
    }

    private Consumers() {}
}
