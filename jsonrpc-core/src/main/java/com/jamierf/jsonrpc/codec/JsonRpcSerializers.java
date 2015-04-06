package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.jamierf.jsonrpc.api.*;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

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
    public class JsonRpcResponseSerializer extends JsonSerializer<JsonRpcResponse<?>> {
        @Override
        public void serialize(final JsonRpcResponse<?> value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("jsonrpc", JsonRpcModule.PROTOCOL_VERSION);

            if (value.getId() != null) {
                gen.writeStringField("id", value.getId());
            }

            final Optional<? extends Result<?>> result = value.getResult();
            final Optional<? extends ErrorMessage> error = value.getError();
            checkArgument(result.isPresent() ^ error.isPresent(), "Only one of result and error may be present");

            if (error.isPresent()) {
                gen.writeObjectField("error", error.get());
            }

            if (result.isPresent()) {
                gen.writeObjectField("result", result.get().get());
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

        if (JsonRpcResponse.class.isAssignableFrom(rawType)) {
            return new JsonRpcResponseSerializer();
        }

        return super.findSerializer(config, type, beanDesc);
    }
}
