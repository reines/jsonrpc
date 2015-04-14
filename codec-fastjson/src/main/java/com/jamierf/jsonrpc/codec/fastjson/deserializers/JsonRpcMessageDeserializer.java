package com.jamierf.jsonrpc.codec.fastjson.deserializers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.util.TypeUtils;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.jamierf.jsonrpc.api.*;
import com.jamierf.jsonrpc.util.TypeReference;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkArgument;

public class JsonRpcMessageDeserializer implements ObjectDeserializer {

    private final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper;
    private final Function<String, TypeReference<?>> responseTypeMapper;
    private final MetricRegistry metrics;

    public JsonRpcMessageDeserializer(final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper,
                                      final Function<String, TypeReference<?>> responseTypeMapper, final MetricRegistry metrics) {
        this.requestParamTypeMapper = requestParamTypeMapper;
        this.responseTypeMapper = responseTypeMapper;
        this.metrics = metrics;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialze(final DefaultJSONParser parser, final Type type, final Object fieldName) {
        final Timer.Context timer = metrics.timer(name(JsonRpcMessageDeserializer.class, "deserialize")).time();
        try {
            final JSONLexer lexer = parser.getLexer();

            if (lexer.token() == JSONToken.NULL) {
                lexer.nextToken();
                return null;
            }

            parser.accept(JSONToken.LBRACE);

            // Shared
            String id = null;
            String protocol = null;

            // JsonRpcRequest
            String method = null;
            Object params = null;

            // JsonRpcResponse
            ErrorMessage<?> error = null;
            Object result = null;

            while (true) {
                final String key = lexer.stringVal();
                lexer.nextToken(JSONToken.COLON);
                parser.accept(JSONToken.COLON);

                switch (key) {
                    case "id":
                        if (lexer.token() != JSONToken.LITERAL_STRING) {
                            throw new JSONException("id is not string");
                        }
                        id = lexer.stringVal();
                        lexer.nextToken();
                        break;
                    case "jsonrpc":
                        if (lexer.token() != JSONToken.LITERAL_STRING) {
                            throw new JSONException("jsonrpc is not string");
                        }
                        protocol = lexer.stringVal();
                        lexer.nextToken();
                        break;
                    case "method":
                        if (lexer.token() != JSONToken.LITERAL_STRING) {
                            throw new JSONException("method is not string");
                        }
                        method = lexer.stringVal();
                        lexer.nextToken();
                        break;
                    case "params":
                        params = parser.parse();
                        break;
                    case "error":
                        error = parser.parseObject(ErrorMessage.class);
                        break;
                    case "result":
                        result = parser.parse();
                        break;
                    default:
                        parser.parse();
                        break;
                }

                if (lexer.token() == JSONToken.COMMA) {
                    lexer.nextToken();
                    continue;
                }

                break;
            }

            parser.accept(JSONToken.RBRACE);

            checkArgument(JsonRpcMessage.PROTOCOL_VERSION.equals(protocol), "Unsupported protocol: " + protocol);

            // This must be a request
            if (method != null) {
                final Parameters<String, TypeReference<?>> types = requestParamTypeMapper.apply(method);
                return (T) new JsonRpcRequest(
                        method,
                        params == null ? Parameters.none() : deserializeParams(params, types),
                        id
                );
            }

            // This must be a response
            if (result != null || error != null) {
                final TypeReference<?> responseType = id != null ? responseTypeMapper.apply(id) : new TypeReference<Object>() {};
                return (T) new JsonRpcResponse<>(
                        result == null ? Optional.absent() : Optional.of(deserializeResult(result, responseType)),
                        Optional.fromNullable(error),
                        id
                );
            }

            // This is an invalid message
            throw new JSONException("Invalid JSON-RPC message");
        } finally {
            timer.stop();
        }
    }

    private Parameters<String, ?> deserializeParams(final Object params, final Parameters<String, TypeReference<?>> types) {
        if (params instanceof JSONArray) {
            return deserializePositionalParameters((JSONArray) params, types);
        }

        if (params instanceof JSONObject) {
            return deserializeNamedParameters((JSONObject) params, types);
        }

        throw new JSONException("Invalid params type");
    }

    private Parameters<String, ?> deserializeNamedParameters(final JSONObject params, final Parameters<String, TypeReference<?>> types)  {
        final Map<String, Object> decodedParameters = Maps.newHashMap();

        // Decode to a temporary map in whatever order we received them
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            final String name = entry.getKey();
            final Object value = entry.getValue();

            final Optional<TypeReference<?>> type = types.get(name);
            checkArgument(type.isPresent(), "Unknown parameter: " + name);

            decodedParameters.put(name, TypeUtils.cast(value, type.get().getType(), null));
        }

        // Create a parameter map in the correct order for the target method
        final Parameters.Builder<String, Object> parameters = Parameters.builder();

        for (final String name : types.keys()) {
            parameters.add(name, decodedParameters.get(name));
        }

        return parameters.build();
    }

    private Parameters<String, ?> deserializePositionalParameters(final JSONArray params,
                                                                  final Parameters<String, TypeReference<?>> types) {
        final Parameters.Builder<String, Object> parameters = Parameters.builder();

        final Iterator<?> nodeIterator = params.iterator();
        final Iterator<Map.Entry<String,TypeReference<?>>> typeIterator = types.iterator();
        while (nodeIterator.hasNext() && typeIterator.hasNext()) {
            final Map.Entry<String, TypeReference<?>> entry = typeIterator.next();

            final String name = entry.getKey();
            final Object value = nodeIterator.next();
            final TypeReference<?> type = entry.getValue();

            parameters.add(name, TypeUtils.cast(value, type.getType(), null));
        }

        checkArgument(!(nodeIterator.hasNext() || typeIterator.hasNext()), "Mismatched number of parameters");
        return parameters.build();
    }

    private <T> Result<T> deserializeResult(final Object result, final TypeReference<?> type) {
        return new Result<>(TypeUtils.cast(result, type.getType(), null));
    }

    @Override
    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }
}
