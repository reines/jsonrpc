package com.jamierf.jsonrpc.codec.jackson.serializers;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.IOException;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;

public class JsonRpcRequestSerializer extends JsonSerializer<JsonRpcRequest> {

    private final boolean useNamedParameters;
    private final MetricRegistry metrics;

    public JsonRpcRequestSerializer(final boolean useNamedParameters, final MetricRegistry metrics) {
        this.useNamedParameters = useNamedParameters;
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
                gen.writeObjectField("params", useNamedParameters ? value.getParams().named() : value.getParams().positional());
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
