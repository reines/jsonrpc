package com.jamierf.jsonrpc.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.base.MoreObjects;

public class JsonRpcRequest extends JsonRpcMessage {

    public static JsonRpcRequest method(final String method, final Map<String, ?> metadata) {
        return method(method, Parameters.none(), metadata);
    }

    public static JsonRpcRequest method(final String method, final Parameters<String, ?> params, final Map<String, ?> metadata) {
        return new JsonRpcRequest(method, params, UUID.randomUUID().toString(), metadata);
    }

    private final String method;
    private final Parameters<String, ?> params;

    public JsonRpcRequest(final String method, final Parameters<String, ?> params, final String id, final Map<String, ?> metadata) {
        super(id, metadata);

        this.method = method;
        this.params = params;
    }

    public String getMethod() {
        return method;
    }

    public Parameters<String, ?> getParams() {
        return params;
    }

    public <T> JsonRpcResponse<T> response(final Result<T> result, final Map<String, ?> metadata) {
        return new JsonRpcResponse<>( Optional.of(result), Optional.empty(), getId(), metadata);
    }

    public JsonRpcResponse<?> error(final int code, final String message, final Map<String, ?> metadata) {
        final ErrorMessage errorMessage = new ErrorMessage<>(code, message, Optional.empty());
        return new JsonRpcResponse<>(Optional.empty(), Optional.of(errorMessage), getId(), metadata);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("method", method)
                .add("params", params)
                .toString();
    }
}
