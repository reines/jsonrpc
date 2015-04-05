package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.google.common.annotations.VisibleForTesting;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.Parameters;

import java.io.IOException;

public class JsonRpcSerializers extends Serializers.Base {

    @VisibleForTesting
    public class JsonRpcRequestSerializer extends JsonSerializer<JsonRpcRequest> {
        @Override
        public void serialize(final JsonRpcRequest value, final JsonGenerator gen, final SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeStringField("jsonrpc", JsonRpcModule.PROTOCOL_VERSION);

            if (value.getId() != null) {
                gen.writeStringField("id", value.getId());
            }
            gen.writeStringField("method", value.getMethod());

            if (!value.getParams().isEmpty()) {
                gen.writeObjectField("params", value.getParams());
            }

            gen.writeEndObject();
        }
    }

    @VisibleForTesting
    public class ParametersSerializer extends JsonSerializer<Parameters> {
        @Override
        public void serialize(final Parameters value, final JsonGenerator gen, final SerializerProvider serializers)
                throws IOException {
            gen.writeObject(useNamedParameters ? value.named() : value.positional());
        }
    }

    private final boolean useNamedParameters;

    public JsonRpcSerializers(final boolean useNamedParameters) {
        this.useNamedParameters = useNamedParameters;
    }

    @Override
    public JsonSerializer<?> findSerializer(final SerializationConfig config, final JavaType type, final BeanDescription beanDesc) {
        final Class<?> rawType = type.getRawClass();
        if (Parameters.class.isAssignableFrom(rawType)) {
            return new ParametersSerializer();
        }

        if (JsonRpcRequest.class.isAssignableFrom(rawType)) {
            return new JsonRpcRequestSerializer();
        }

        return super.findSerializer(config, type, beanDesc);
    }
}
