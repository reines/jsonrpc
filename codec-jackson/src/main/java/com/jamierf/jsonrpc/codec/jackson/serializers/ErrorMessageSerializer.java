package com.jamierf.jsonrpc.codec.jackson.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Optional;
import com.jamierf.jsonrpc.api.ErrorMessage;

import java.io.IOException;

public class ErrorMessageSerializer extends JsonSerializer<ErrorMessage> {
    @Override
    public void serialize(final ErrorMessage value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeNumberField("code", value.getCode());
        gen.writeStringField("message", value.getMessage());

        final Optional<?> data = value.getData();
        if (data.isPresent()) {
            gen.writeObjectField("data", data.get());
        }

        gen.writeEndObject();
    }
}
