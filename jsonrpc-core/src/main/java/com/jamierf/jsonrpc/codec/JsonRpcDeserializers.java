package com.jamierf.jsonrpc.codec;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.google.common.base.Function;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.codec.deserializers.JsonRpcMessageDeserializer;
import com.jamierf.jsonrpc.codec.deserializers.JsonRpcRequestDeserializer;
import com.jamierf.jsonrpc.codec.deserializers.JsonRpcResponseDeserializer;

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
