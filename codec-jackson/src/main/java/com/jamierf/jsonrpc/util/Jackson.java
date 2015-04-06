package com.jamierf.jsonrpc.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Type;

public final class Jackson {
    public static int getInt(final JsonNode node, final String key) {
        final JsonNode value = get(node, key);
        return value == null ? 0 : value.asInt();
    }

    public static String getText(final JsonNode node, final String key) {
        final JsonNode value = get(node, key);
        return value == null ? null : value.asText();
    }

    public static JsonNode get(final JsonNode node, final String key) {
        final JsonNode value = node.get(key);
        return value == null || value.isNull() ? null : value;
    }

    public static <T> TypeReference<T> reference(final Type type) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return type;
            }
        };
    }

    public static <T> TypeReference<T> reference(final com.jamierf.jsonrpc.util.TypeReference<T> type) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return type.getType();
            }
        };
    }

    private Jackson() {
    }
}
