package com.jamierf.jsonrpc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.util.Reflections;

import java.lang.reflect.Type;

import static com.jamierf.jsonrpc.util.Reflections.parameterizedType;

public class PendingResponse<T> {

    public static <T> PendingResponse<T> expectedType(final Class<T> type, final Class<?>... argTypes) {
        return new PendingResponse<>(parameterizedType(type, argTypes));
    }

    private final TypeReference<T> type;
    private final SettableFuture<T> future;

    public PendingResponse(final Type type) {
        this.type = Reflections.reference(type);
        this.future = SettableFuture.create();

        // If no response is expected, immediately return null
        if (Void.class.isAssignableFrom(Reflections.classOf(type))) {
            future.set(null);
        }
    }

    public ListenableFuture<T> getFuture() {
        return future;
    }

    public TypeReference<T> getType() {
        return type;
    }

    public void complete(final JsonRpcResponse<T> response) {
        if (response.getError().isPresent()) {
            final ErrorMessage error = response.getError().get();
            future.setException(new RuntimeException(error.getMessage())); // TODO
        } else if (response.getResult().isPresent()) {
            final T result = response.getResult().get();
            future.set(result);
        } else {
            throw new IllegalStateException("Response doesn't include a result or error.");
        }
    }
}
