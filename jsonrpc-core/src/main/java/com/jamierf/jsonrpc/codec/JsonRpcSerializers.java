package com.jamierf.jsonrpc.codec;

import com.codahale.metrics.MetricRegistry;
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
    private final MetricRegistry metrics;

    public JsonRpcSerializers(final boolean useNamedParameters, final MetricRegistry metrics) {
        this.useNamedParameters = useNamedParameters;
        this.metrics = metrics;
    }

    @Override
    public JsonSerializer<?> findSerializer(final SerializationConfig config, final JavaType type, final BeanDescription beanDesc) {
        final Class<?> rawType = type.getRawClass();

        if (Parameters.class.isAssignableFrom(rawType)) {
            return new ParametersSerializer(useNamedParameters);
        }

        if (JsonRpcRequest.class.isAssignableFrom(rawType)) {
            return new JsonRpcRequestSerializer(metrics);
        }

        if (JsonRpcResponse.class.isAssignableFrom(rawType)) {
            return new JsonRpcResponseSerializer(metrics);
        }

        return super.findSerializer(config, type, beanDesc);
    }
}
