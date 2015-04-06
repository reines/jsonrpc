package com.jamierf.jsonrpc.codec;

import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.Parameters;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

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

    public static Matcher<JsonRpcRequest> withParams(final Parameters<String, ?> params) {
        return new FeatureMatcher<JsonRpcRequest, Parameters<String, ?>>(equalTo(params), "method params", "method params") {
            @Override
            protected Parameters<String, ?> featureValueOf(final JsonRpcRequest actual) {
                return actual.getParams();
            }
        };
    }

    private JsonRpcRequestMatchers() {
    }
}
