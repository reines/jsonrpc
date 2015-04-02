package com.jamierf.jsonrpc.util;

import com.fasterxml.jackson.databind.JsonNode;

public final class Nodes {

    public static String getText(final JsonNode node, final String key) {
        final JsonNode value = get(node, key);
        return value == null ? null : value.asText();
    }

    public static JsonNode get(final JsonNode node, final String key) {
        final JsonNode value = node.get(key);
        return value == null || value.isNull() ? null : value;
    }

    private Nodes() {
    }
}
