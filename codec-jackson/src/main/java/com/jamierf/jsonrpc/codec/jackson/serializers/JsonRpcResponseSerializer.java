package com.jamierf.jsonrpc.codec.jackson.serializers;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.Optional;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Result;

public class JsonRpcResponseSerializer extends JsonSerializer<JsonRpcResponse<?>> {

    private final MetricRegistry metrics;

    public JsonRpcResponseSerializer(final MetricRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public void serialize(final JsonRpcResponse<?> value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        final Timer.Context timer = metrics.timer(name(JsonRpcResponseSerializer.class, "serialize")).time();
        try {
            gen.writeStartObject();
            gen.writeStringField("jsonrpc", JsonRpcMessage.PROTOCOL_VERSION);

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

            if (!value.getMetadata().isEmpty()) {
                gen.writeObjectField( "meta", value.getMetadata() );
            }

            gen.writeEndObject();
        } finally {
            timer.stop();
        }
    }
}
