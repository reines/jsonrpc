package com.jamierf.jsonrpc.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

public class JsonRpcResponse<T> extends JsonRpcMessage {

    private final Optional<T> result;
    private final Optional<ErrorMessage> error;

    public JsonRpcResponse(final Optional<T> result, final Optional<ErrorMessage> error, final String id) {
        super(id);

        this.result = result;
        this.error = error;
    }

    public Optional<T> getResult() {
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
