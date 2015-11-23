package com.jamierf.jsonrpc.codec.jackson;

import java.util.function.Function;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.codec.jackson.deserializers.ErrorMessageDeserializer;
import com.jamierf.jsonrpc.codec.jackson.deserializers.JsonRpcMessageDeserializer;
import com.jamierf.jsonrpc.codec.jackson.deserializers.JsonRpcRequestDeserializer;
import com.jamierf.jsonrpc.codec.jackson.deserializers.JsonRpcResponseDeserializer;
import com.jamierf.jsonrpc.util.TypeReference;

public class JsonRpcDeserializers extends Deserializers.Base {

    private final Function<String, TypeReference<?>> responseTypeMapper;
    private final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper;
    private final MetricRegistry metrics;

    public JsonRpcDeserializers(final Function<String, TypeReference<?>> responseTypeMapper,
                                final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper,
                                final MetricRegistry metrics) {
        this.responseTypeMapper = responseTypeMapper;
        this.requestParamTypeMapper = requestParamTypeMapper;
        this.metrics = metrics;
    }

    @Override
    public JsonDeserializer<?> findBeanDeserializer(final JavaType type, final DeserializationConfig config,
                                                    final BeanDescription beanDesc) throws JsonMappingException {
        final Class<?> rawType = type.getRawClass();

        if (ErrorMessage.class.isAssignableFrom(rawType)) {
            return new ErrorMessageDeserializer();
        }

        if (JsonRpcRequest.class.isAssignableFrom(rawType)) {
            return new JsonRpcRequestDeserializer(requestParamTypeMapper, metrics);
        }

        if (JsonRpcResponse.class.isAssignableFrom(rawType)) {
            return new JsonRpcResponseDeserializer(responseTypeMapper, metrics);
        }

        if (JsonRpcMessage.class.isAssignableFrom(rawType)) {
            return new JsonRpcMessageDeserializer();
        }

        return super.findBeanDeserializer(type, config, beanDesc);
    }
}
