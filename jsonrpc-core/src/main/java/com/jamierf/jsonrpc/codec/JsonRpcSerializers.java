package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.codec.serializers.JsonRpcRequestSerializer;
import com.jamierf.jsonrpc.codec.serializers.JsonRpcResponseSerializer;
import com.jamierf.jsonrpc.codec.serializers.ParametersSerializer;

public class JsonRpcSerializers extends Serializers.Base {

    private final boolean useNamedParameters;

    public JsonRpcSerializers(final boolean useNamedParameters) {
        this.useNamedParameters = useNamedParameters;
    }

    @Override
    public JsonSerializer<?> findSerializer(final SerializationConfig config, final JavaType type, final BeanDescription beanDesc) {
        final Class<?> rawType = type.getRawClass();

        if (Parameters.class.isAssignableFrom(rawType)) {
            return new ParametersSerializer(useNamedParameters);
        }

        if (JsonRpcRequest.class.isAssignableFrom(rawType)) {
            return new JsonRpcRequestSerializer();
        }

        if (JsonRpcResponse.class.isAssignableFrom(rawType)) {
            return new JsonRpcResponseSerializer();
        }

        return super.findSerializer(config, type, beanDesc);
    }
}
