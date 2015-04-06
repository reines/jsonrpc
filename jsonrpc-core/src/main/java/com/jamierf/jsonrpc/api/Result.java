package com.jamierf.jsonrpc.api;

public class Result<T> {

    private final T result;

    public Result(final T result) {
        this.result = result;
    }

    public T get() {
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(result);
    }
}
