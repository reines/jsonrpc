package com.jamierf.jsonrpc.api;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;

public final class JsonRpcRequestMatchers {
    public static Matcher<JsonRpcRequest> forMethod(final String method) {
        return new FeatureMatcher<JsonRpcRequest, String>(equalTo(method), "method name", "method name") {
            @Override
            protected String featureValueOf(final JsonRpcRequest actual) {
                return actual.getMethod();
            }
        };
    }

    public static Matcher<JsonRpcRequest> withParams(final Map<String, ?> params) {
        return new FeatureMatcher<JsonRpcRequest, Map<String, ?>>(equalTo(params), "method params", "method params") {
            @Override
            protected Map<String, ?> featureValueOf(final JsonRpcRequest actual) {
                return actual.getParams();
            }
        };
    }

    private JsonRpcRequestMatchers() {
    }
}
