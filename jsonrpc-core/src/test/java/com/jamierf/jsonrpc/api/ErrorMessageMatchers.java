package com.jamierf.jsonrpc.api;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.core.IsEqual.equalTo;

public final class ErrorMessageMatchers {
    public static Matcher<ErrorMessage> withCode(final int code) {
        return new FeatureMatcher<ErrorMessage, Integer>(equalTo(code), "error code", "error code") {
            @Override
            protected Integer featureValueOf(final ErrorMessage actual) {
                return actual.getCode();
            }
        };
    }

    public static Matcher<ErrorMessage> withMessage(final String message) {
        return new FeatureMatcher<ErrorMessage, String>(equalTo(message), "error message", "error message") {
            @Override
            protected String featureValueOf(final ErrorMessage actual) {
                return actual.getMessage();
            }
        };
    }

    private ErrorMessageMatchers() {
    }
}
