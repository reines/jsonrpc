package com.jamierf.jsonrpc.codec.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Optional;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Result;
import com.jamierf.jsonrpc.codec.JsonRpcModule;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

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
