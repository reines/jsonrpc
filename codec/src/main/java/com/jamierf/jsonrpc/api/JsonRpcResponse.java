package com.jamierf.jsonrpc.api;

import java.util.Map;
import java.util.Optional;

import com.google.common.base.MoreObjects;

public class JsonRpcResponse<T> extends JsonRpcMessage {

    public static JsonRpcResponse<?> error(final int code, final String message, final Map<String, ?> metadata) {
        return new JsonRpcResponse<>(Optional.empty(), Optional.of(new ErrorMessage<>(code, message, Optional.empty())), null, metadata);
    }

    private final Optional<Result<T>> result;
    private final Optional<ErrorMessage> error;

    public JsonRpcResponse(final Optional<Result<T>> result, final Optional<ErrorMessage> error, final String id, final Map<String, ?> metadata) {
        super(id, metadata);

        this.result = result;
        this.error = error;
    }

    public Optional<Result<T>> getResult() {
        return result;
    }

    public Optional<ErrorMessage> getError() {
        return error;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("result", result)
                .add("error", error)
                .toString();
    }
}
