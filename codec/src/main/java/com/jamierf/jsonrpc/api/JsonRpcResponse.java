package com.jamierf.jsonrpc.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

public class JsonRpcResponse<T> extends JsonRpcMessage {

    public static JsonRpcResponse<?> error(final int code, final String message) {
        return new JsonRpcResponse<>(Optional.absent(), Optional.of(new ErrorMessage<>(code, message, Optional.absent())), null);
    }

    private final Optional<Result<T>> result;
    private final Optional<ErrorMessage> error;

    public JsonRpcResponse(final Optional<Result<T>> result, final Optional<ErrorMessage> error, final String id) {
        super(id);

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
