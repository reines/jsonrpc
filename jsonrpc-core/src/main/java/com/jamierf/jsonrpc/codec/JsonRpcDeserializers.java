package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.util.Nodes;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.jamierf.jsonrpc.util.Jackson.deserialize;

public class JsonRpcDeserializers extends Deserializers.Base {

    @VisibleForTesting
    public class JsonRpcMessageDeserializer extends JsonDeserializer<JsonRpcMessage> {
        @Override
        public JsonRpcMessage deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
            final ObjectCodec codec = checkNotNull(jp.getCodec());
            final JsonNode node = codec.readTree(jp);

            // This must be a request
            if (node.has("method")) {
                return deserializeRequest(node, codec);
            }

            // This must be a response
            if (node.has("result") || node.has("error")) {
                return deserializeResponse(node, codec);
            }

            // This is an invalid message
            throw new JsonParseException("Invalid JSON-RPC message.", jp.getCurrentLocation());
        }
    }

    private final Function<String, TypeReference<?>> responseTypeMapper;
    private final Function<String, Map<String, TypeReference<?>>> requestParamTypeMapper;

    public JsonRpcDeserializers(final Function<String, TypeReference<?>> responseTypeMapper,
                                final Function<String, Map<String, TypeReference<?>>> requestParamTypeMapper) {
        this.responseTypeMapper = responseTypeMapper;
        this.requestParamTypeMapper = requestParamTypeMapper;
    }

    @Override
    public JsonDeserializer<?> findBeanDeserializer(final JavaType type, final DeserializationConfig config,
                                                    final BeanDescription beanDesc) throws JsonMappingException {
        if (JsonRpcMessage.class.isAssignableFrom(type.getRawClass())) {
            return new JsonRpcMessageDeserializer();
        }

        return super.findBeanDeserializer(type, config, beanDesc);
    }

    private JsonRpcRequest deserializeRequest(final JsonNode node, final ObjectCodec codec) throws IOException {
        // Extract the ID then figure out the parameter types based on the known method parameters
        final Optional<String> method = Optional.fromNullable(Nodes.getText(node, "method"));
        checkArgument(method.isPresent(), "Invalid request without method");
        final Map<String, TypeReference<?>> types = requestParamTypeMapper.apply(method.get());

        final Optional<String> id = Optional.fromNullable(Nodes.getText(node, "id"));
        final Optional<JsonNode> params = Optional.fromNullable(Nodes.get(node, "params"));

        return new JsonRpcRequest(
                Nodes.getText(node, "jsonrpc"),
                method.get(),
                params.isPresent() ? deserializeParams(params.get(), types, codec) : Collections.emptyMap(),
                id.orNull()
        );
    }

    private Map<String, ?> deserializeParams(final JsonNode nodes, final Map<String, TypeReference<?>> types, final ObjectCodec codec) throws IOException {
        final ImmutableMap.Builder<String, Object> params = ImmutableMap.builder();

        final Iterator<Map.Entry<String, JsonNode>> nodeIterator = nodes.fields();
        while (nodeIterator.hasNext()) {
            final Map.Entry<String, JsonNode> entry = nodeIterator.next();

            final String name = entry.getKey();
            final JsonParser jp = codec.treeAsTokens(entry.getValue());

            final TypeReference<?> type = types.get(name);
            checkArgument(type != null, "Unknown parameter: " + name);

            params.put(name, codec.readValue(jp, type));
        }

        return params.build();
    }

    private JsonRpcResponse<?> deserializeResponse(final JsonNode node, final ObjectCodec codec) throws IOException {
        // Extract the ID then figure out the type based on the expected return type of the request
        final Optional<String> id = Optional.fromNullable(Nodes.getText(node, "id"));

        final Optional<JsonNode> result = Optional.fromNullable(Nodes.get(node, "result"));
        final Optional<JsonNode> error = Optional.fromNullable(Nodes.get(node, "error"));
        checkArgument(result.isPresent() ^ error.isPresent(), "Only one of result and error may be present");

        checkArgument(id.isPresent() || !result.isPresent(), "Invalid result response without id");
        final TypeReference<?> type = id.isPresent() ? responseTypeMapper.apply(id.get()) : new TypeReference<Object>() {};

        return new JsonRpcResponse<>(
                Nodes.getText(node, "jsonrpc"),
                result.transform(deserialize(codec, type)),
                error.transform(deserialize(codec, ErrorMessage.class)),
                id.orNull()
        );
    }
}
