package com.jamierf.jsonrpc.error;

import com.jamierf.jsonrpc.api.ErrorMessage;

public class CodedException extends RuntimeException {

    public static CodedException fromErrorMessage(final ErrorMessage error) {
        return new CodedException(error.getCode(), error.getMessage());
    }

    private final int code;

    public CodedException(final int code, final String message) {
        super (message);

        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return String.format("[%d]: %s", code, getMessage());
    }
}
