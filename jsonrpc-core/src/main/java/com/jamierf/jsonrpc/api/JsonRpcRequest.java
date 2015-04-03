package com.jamierf.jsonrpc.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class JsonRpcRequest extends JsonRpcMessage {

    public static JsonRpcRequest method(final String method) {
        return method(method, Collections.emptyMap());
    }

    public static JsonRpcRequest method(final String method, final Map<String, ?> params) {
        return new JsonRpcRequest("2.0", method, params, UUID.randomUUID().toString());
    }

    private final String method;
    private final Map<String, ?> params;

    public JsonRpcRequest(final String protocol, final String method, final Map<String, ?> params, final String id) {
        super(protocol, id);

        this.method = method;
        this.params = params;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, ?> getParams() {
        return params;
    }

    public <T> JsonRpcResponse<T> response(final T result) {
        return new JsonRpcResponse<>(getProtocol(), Optional.of(result), Optional.absent(), getId());
    }

    public JsonRpcResponse<?> error(final int code, final String message) {
        final ErrorMessage errorMessage = new ErrorMessage(code, message);
        return new JsonRpcResponse<>(getProtocol(), Optional.absent(), Optional.of(errorMessage), getId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("method", method)
                .add("params", params)
                .toString();
    }
}
