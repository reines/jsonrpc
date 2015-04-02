package com.jamierf.jsonrpc.util;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.google.common.base.Function;
import com.google.common.base.Throwables;

import java.io.IOException;

public final class Jackson {
    public static ObjectMapper newObjectMapper() {
        return new ObjectMapper()
                .registerModule(new GuavaModule())
                .registerModule(new AfterburnerModule())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    }

    public static <T> Function<JsonNode, T> deserialize(final ObjectCodec codec, final Class<T> type) {
        return input -> {
            try {
                return codec.readValue(codec.treeAsTokens(input), type);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        };
    }

    public static <T> Function<JsonNode, T> deserialize(final ObjectCodec codec, final TypeReference<T> type) {
        return input -> {
            try {
                return codec.readValue(codec.treeAsTokens(input), type);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        };
    }

    private Jackson() {
    }
}
