package com.jamierf.jsonrpc.codec.jackson;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.codec.jackson.serializers.ErrorMessageSerializer;
import com.jamierf.jsonrpc.codec.jackson.serializers.JsonRpcRequestSerializer;
import com.jamierf.jsonrpc.codec.jackson.serializers.JsonRpcResponseSerializer;

public class JsonRpcSerializers extends Serializers.Base {

    private final boolean useNamedParameters;
    private final MetricRegistry metrics;

    public JsonRpcSerializers(final boolean useNamedParameters, final MetricRegistry metrics) {
        this.useNamedParameters = useNamedParameters;
        this.metrics = metrics;
    }

    @Override
    public JsonSerializer<?> findSerializer(final SerializationConfig config, final JavaType type, final BeanDescription beanDesc) {
        final Class<?> rawType = type.getRawClass();

        if (JsonRpcRequest.class.isAssignableFrom(rawType)) {
            return new JsonRpcRequestSerializer(useNamedParameters, metrics);
        }

        if (JsonRpcResponse.class.isAssignableFrom(rawType)) {
            return new JsonRpcResponseSerializer(metrics);
        }

        if (ErrorMessage.class.isAssignableFrom(rawType)) {
            return new ErrorMessageSerializer();
        }

        return super.findSerializer(config, type, beanDesc);
    }
}
