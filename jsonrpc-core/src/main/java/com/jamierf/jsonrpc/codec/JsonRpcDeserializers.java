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
import com.google.common.collect.Maps;
import com.jamierf.jsonrpc.api.*;
import com.jamierf.jsonrpc.util.Nodes;

import java.io.IOException;
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

            final String protocol = Nodes.getText(node, "jsonrpc");
            checkArgument(JsonRpcModule.PROTOCOL_VERSION.equals(protocol), "Unsupported protocol: " + protocol);

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
    private final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper;

    public JsonRpcDeserializers(final Function<String, TypeReference<?>> responseTypeMapper,
                                final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper) {
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
        final Parameters<String, TypeReference<?>> types = requestParamTypeMapper.apply(method.get());

        final Optional<String> id = Optional.fromNullable(Nodes.getText(node, "id"));
        final Optional<JsonNode> params = Optional.fromNullable(Nodes.get(node, "params"));

        return new JsonRpcRequest(
                method.get(),
                params.isPresent() ? deserializeParams(params.get(), types, codec) : Parameters.none(),
                id.orNull()
        );
    }

    private Parameters deserializeParams(final JsonNode nodes, final Parameters<String, TypeReference<?>> types,
                                         final ObjectCodec codec) throws IOException {
        if (nodes.isArray()) {
            return deserializePositionalParameters(nodes, types, codec);
        }

        return deserializeNamedParameters(nodes, types, codec);
    }

    private Parameters deserializeNamedParameters(final JsonNode nodes, final Parameters<String, TypeReference<?>> types,
                                                  final ObjectCodec codec) throws IOException {
        final Map<String, Object> decodedParameters = Maps.newHashMap();

        // Decode to a temporary map in whatever order we received them
        final Iterator<Map.Entry<String, JsonNode>> nodeIterator = nodes.fields();
        while (nodeIterator.hasNext()) {
            final Map.Entry<String, JsonNode> entry = nodeIterator.next();

            final String name = entry.getKey();
            final JsonParser jp = codec.treeAsTokens(entry.getValue());

            final Optional<TypeReference<?>> type = types.get(name);
            checkArgument(type.isPresent(), "Unknown parameter: " + name);

            decodedParameters.put(name, codec.readValue(jp, type.get()));
        }

        // Create a parameter map in the correct order for the target method
        final Parameters.Builder<String, Object> parameters = Parameters.builder();

        for (final String name : types.keys()) {
            parameters.add(name, decodedParameters.get(name));
        }

        return parameters.build();
    }

    private Parameters deserializePositionalParameters(final JsonNode nodes, final Parameters<String, TypeReference<?>> types,
                                                       final ObjectCodec codec) throws IOException {
        final Parameters.Builder<String, Object> parameters = Parameters.builder();

        final Iterator<JsonNode> nodeIterator = nodes.elements();
        final Iterator<Map.Entry<String,TypeReference<?>>> typeIterator = types.iterator();
        while (nodeIterator.hasNext() && typeIterator.hasNext()) {
            final Map.Entry<String, TypeReference<?>> entry = typeIterator.next();

            final String name = entry.getKey();
            final JsonParser jp = codec.treeAsTokens(nodeIterator.next());

            final TypeReference<?> type = entry.getValue();

            parameters.add(name, codec.readValue(jp, type));
        }

        checkArgument(!(nodeIterator.hasNext() || typeIterator.hasNext()), "Mismatched number of parameters");
        return parameters.build();
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
                result.transform(deserialize(codec, type)).transform(Result::new),
                error.transform(deserialize(codec, ErrorMessage.class)),
                id.orNull()
        );
    }
}
