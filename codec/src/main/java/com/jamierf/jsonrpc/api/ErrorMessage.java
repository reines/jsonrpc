package com.jamierf.jsonrpc.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

public class ErrorMessage<T> {

    public static final int CODE_PARSE_ERROR = -32700;
    public static final int CODE_INVALID_REQUEST = -32600;
    public static final int CODE_METHOD_NOT_FOUND = -32601;
    public static final int CODE_INVALID_PARAMS = -32602;
    public static final int CODE_INTERNAL_ERROR = -32603;

    private final int code;
    private final String message;
    private final Optional<T> data;

    public ErrorMessage(final int code, final String message, final Optional<T> data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Optional<T> getData() {
        return data;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("code", code)
                .add("message", message)
                .add("data", data)
                .toString();
    }
}
