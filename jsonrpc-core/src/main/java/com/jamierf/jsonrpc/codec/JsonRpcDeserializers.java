package com.jamierf.jsonrpc.codec;

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

    public JsonRpcDeserializers(final Function<String, TypeReference<?>> responseTypeMapper,
                                final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper) {
        this.responseTypeMapper = responseTypeMapper;
        this.requestParamTypeMapper = requestParamTypeMapper;
    }

    @Override
    public JsonDeserializer<?> findBeanDeserializer(final JavaType type, final DeserializationConfig config,
                                                    final BeanDescription beanDesc) throws JsonMappingException {
        final Class<?> rawType = type.getRawClass();

        if (JsonRpcRequest.class.isAssignableFrom(rawType)) {
            return new JsonRpcRequestDeserializer(requestParamTypeMapper);
        }

        if (JsonRpcResponse.class.isAssignableFrom(rawType)) {
            return new JsonRpcResponseDeserializer(responseTypeMapper);
        }

        if (JsonRpcMessage.class.isAssignableFrom(rawType)) {
            return new JsonRpcMessageDeserializer();
        }

        return super.findBeanDeserializer(type, config, beanDesc);
    }
}
