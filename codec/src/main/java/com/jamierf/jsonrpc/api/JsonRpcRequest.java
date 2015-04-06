package com.jamierf.jsonrpc.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import java.util.UUID;

public class JsonRpcRequest extends JsonRpcMessage {

    public static JsonRpcRequest method(final String method) {
        return method(method, Parameters.none());
    }

    public static JsonRpcRequest method(final String method, final Parameters<String, ?> params) {
        return new JsonRpcRequest(method, params, UUID.randomUUID().toString());
    }

    private final String method;
    private final Parameters<String, ?> params;

    public JsonRpcRequest(final String method, final Parameters<String, ?> params, final String id) {
        super(id);

        this.method = method;
        this.params = params;
    }

    public String getMethod() {
        return method;
    }

    public Parameters<String, ?> getParams() {
        return params;
    }

    public <T> JsonRpcResponse<T> response(final Result<T> result) {
        return new JsonRpcResponse<>(Optional.of(result), Optional.absent(), getId());
    }

    public JsonRpcResponse<?> error(final int code, final String message) {
        final ErrorMessage errorMessage = new ErrorMessage(code, message);
        return new JsonRpcResponse<>(Optional.absent(), Optional.of(errorMessage), getId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("method", method)
                .add("params", params)
                .toString();
    }
}
