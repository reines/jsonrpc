package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.google.common.annotations.VisibleForTesting;
import com.jamierf.jsonrpc.api.JsonRpcRequest;

import java.io.IOException;
import java.util.Map;

public class JsonRpcSerializers extends Serializers.Base {

    @VisibleForTesting
    public class JsonRpcRequestSerializer extends JsonSerializer<JsonRpcRequest> {
        @Override
        public void serialize(final JsonRpcRequest value, final JsonGenerator gen, final SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();

            gen.writeStringField("jsonrpc", value.getProtocol());
            if (value.getId() != null) {
                gen.writeStringField("id", value.getId());
            }
            gen.writeStringField("method", value.getMethod());

            final Map<String, ?> params = value.getParams();
            if (!params.isEmpty()) {
                gen.writeObjectField("params", convertParams(params));
            }

            gen.writeEndObject();
        }
    }

    private final boolean useNamedParameters;

    public JsonRpcSerializers(final boolean useNamedParameters) {
        this.useNamedParameters = useNamedParameters;
    }

    private Object convertParams(final Map<String, ?> params) {
        if (useNamedParameters) {
            return params;
        }

        // TODO: Convert to an array - handle ordering!
        return params.values();
    }

    @Override
    public JsonSerializer<?> findSerializer(final SerializationConfig config, final JavaType type, final BeanDescription beanDesc) {
        if (JsonRpcRequest.class.isAssignableFrom(type.getRawClass())) {
            return new JsonRpcRequestSerializer();
        }

        return super.findSerializer(config, type, beanDesc);
    }
}
