package com.jamierf.jsonrpc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Result;
import com.jamierf.jsonrpc.error.CodedException;
import com.jamierf.jsonrpc.util.Reflections;

import java.lang.reflect.Type;

import static com.jamierf.jsonrpc.util.Reflections.isVoid;

public class PendingResponse<T> {

    private final Type type;
    private final SettableFuture<T> future;

    public PendingResponse(final Type type) {
        this.type = type;
        this.future = SettableFuture.create();

        // If no response is expected, immediately return null
        if (!expectsResponse()) {
            future.set(null);
        }
    }

    public boolean expectsResponse() {
        return !isVoid(Reflections.classOf(type));
    }

    public ListenableFuture<T> getFuture() {
        return future;
    }

    public TypeReference<T> getType() {
        return Reflections.reference(type);
    }

    public synchronized void complete(final JsonRpcResponse<T> response) {
        if (isComplete()) {
            return;
        }

        if (response.getError().isPresent()) {
            final ErrorMessage error = response.getError().get();
            complete(CodedException.fromErrorMessage(error));
        } else if (response.getResult().isPresent()) {
            final Result<T> result = response.getResult().get();
            future.set(result.get());
        } else {
            throw new IllegalStateException("Response doesn't include a result or error.");
        }
    }

    public synchronized void complete(final Throwable exception) {
        if (isComplete()) {
            return;
        }

        future.setException(exception);
    }

    public boolean isComplete() {
        return future.isDone();
    }
}
