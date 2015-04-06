package com.jamierf.jsonrpc.api;

import com.google.common.base.MoreObjects;

public class ErrorMessage {

    public static final int CODE_PARSE_ERROR = -32700;
    public static final int CODE_INVALID_REQUEST = -32600;
    public static final int CODE_METHOD_NOT_FOUND = -32601;
    public static final int CODE_INVALID_PARAMS = -32602;
    public static final int CODE_INTERNAL_ERROR = -32603;

    private final int code;
    private final String message;

    public ErrorMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("code", code)
                .add("message", message)
                .toString();
    }
}
