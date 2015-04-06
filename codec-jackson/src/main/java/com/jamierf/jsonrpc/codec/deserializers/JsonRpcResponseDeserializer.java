package com.jamierf.jsonrpc.codec.deserializers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Result;
import com.jamierf.jsonrpc.util.Jackson;
import com.jamierf.jsonrpc.util.TypeReference;

import java.io.IOException;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.jamierf.jsonrpc.util.Jackson.reference;

public class JsonRpcResponseDeserializer extends JsonDeserializer<JsonRpcResponse<?>> {

    private final Function<String, TypeReference<?>> responseTypeMapper;
    private final MetricRegistry metrics;

    public JsonRpcResponseDeserializer(final Function<String, TypeReference<?>> responseTypeMapper,
                                       final MetricRegistry metrics) {
        this.responseTypeMapper = responseTypeMapper;
        this.metrics = metrics;
    }

    @Override
    public JsonRpcResponse<?> deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
        final Timer.Context timer = metrics.timer(name(JsonRpcResponseDeserializer.class, "deserialize")).time();
        try {
            final ObjectCodec codec = checkNotNull(jp.getCodec());
            final JsonNode node = codec.readTree(jp);

            // Extract the ID then figure out the type based on the expected return type of the request
            final Optional<String> id = Optional.fromNullable(Jackson.getText(node, "id"));

            final Optional<JsonNode> result = Optional.fromNullable(Jackson.get(node, "result"));
            final Optional<JsonNode> error = Optional.fromNullable(Jackson.get(node, "error"));
            checkArgument(result.isPresent() ^ error.isPresent(), "Only one of result and error may be present");

            checkArgument(id.isPresent() || !result.isPresent(), "Invalid result response without id");
            final TypeReference<?> type = id.isPresent() ? responseTypeMapper.apply(id.get()) : new TypeReference<Object>() {};

            return new JsonRpcResponse<>(
                    result.transform(deserialize(codec, reference(type))).transform(Result::new),
                    error.transform(deserialize(codec, reference(ErrorMessage.class))),
                    id.orNull()
            );
        } finally {
            timer.stop();
        }
    }

    private static <T> Function<JsonNode, T> deserialize(final ObjectCodec codec, final com.fasterxml.jackson.core.type.TypeReference<T> type) {
        return input -> {
            try {
                return codec.readValue(codec.treeAsTokens(input), type);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        };
    }
}
