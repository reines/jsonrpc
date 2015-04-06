package com.jamierf.jsonrpc.codec.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.codec.JsonRpcModule;

import java.io.IOException;

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
