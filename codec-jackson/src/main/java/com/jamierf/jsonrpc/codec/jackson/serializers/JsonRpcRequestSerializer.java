package com.jamierf.jsonrpc.codec.jackson.serializers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;

import java.io.IOException;

import static com.codahale.metrics.MetricRegistry.name;

public class JsonRpcRequestSerializer extends JsonSerializer<JsonRpcRequest> {

    private final MetricRegistry metrics;

    public JsonRpcRequestSerializer(final MetricRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public void serialize(final JsonRpcRequest value, final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException {
        final Timer.Context timer = metrics.timer(name(JsonRpcRequestSerializer.class, "serialize")).time();
        try {
            gen.writeStartObject();
            gen.writeStringField("jsonrpc", JsonRpcMessage.PROTOCOL_VERSION);

            if (value.getId() != null) {
                gen.writeStringField("id", value.getId());
            }
            gen.writeStringField("method", value.getMethod());

            if (!value.getParams().isEmpty()) {
                gen.writeObjectField("params", value.getParams());
            }

            gen.writeEndObject();
        } finally {
            timer.stop();
        }
    }
}
