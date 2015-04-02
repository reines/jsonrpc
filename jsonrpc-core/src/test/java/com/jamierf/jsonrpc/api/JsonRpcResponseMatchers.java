package com.jamierf.jsonrpc.api;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsNull.notNullValue;

public final class JsonRpcResponseMatchers {
    public static <T> Matcher<JsonRpcResponse<T>> withError(final Matcher<ErrorMessage> delegate) {
        return new FeatureMatcher<JsonRpcResponse<T>, ErrorMessage>(allOf(notNullValue(), delegate), "error", "error") {
            @Override
            protected ErrorMessage featureValueOf(final JsonRpcResponse<T> actual) {
                return actual.getError().orNull();
            }
        };
    }

    public static <T> Matcher<JsonRpcResponse<T>> withResult(final Matcher<T> delegate) {
        return new FeatureMatcher<JsonRpcResponse<T>, T>(allOf(notNullValue(), delegate), "result", "result") {
            @Override
            protected T featureValueOf(final JsonRpcResponse<T> actual) {
                return actual.getResult().orNull();
            }
        };
    }

    private JsonRpcResponseMatchers() {
    }
}
