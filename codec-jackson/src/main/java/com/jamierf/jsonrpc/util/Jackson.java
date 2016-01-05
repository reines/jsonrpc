package com.jamierf.jsonrpc.util;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

public final class Jackson {

    public static ObjectMapper newObjectMapper() {
        return new ObjectMapper( new JsonFactory() )
            .registerModule( new GuavaModule() )
            .registerModule(new AfterburnerModule());
    }

    private static final ObjectMapper mapper = newObjectMapper();

    public static int getInt(final JsonNode node, final String key) {
        final JsonNode value = get(node, key);
        return value == null ? 0 : value.asInt();
    }

    public static String getText(final JsonNode node, final String key) {
        final JsonNode value = get(node, key);
        return value == null ? null : value.asText();
    }

    public static Map<String, ?> getMap(final JsonNode node, final String key) {
        final JsonNode value = get(node, key);
        if ( value == null ) {
            return Collections.emptyMap();
        }
        return mapper.convertValue( value, new TypeReference<Map<String, ?>>() {} );
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
