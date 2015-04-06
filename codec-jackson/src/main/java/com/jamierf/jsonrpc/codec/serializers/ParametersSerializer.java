package com.jamierf.jsonrpc.codec.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jamierf.jsonrpc.api.Parameters;

import java.io.IOException;

public class ParametersSerializer extends JsonSerializer<Parameters> {

    private final boolean useNamedParameters;

    public ParametersSerializer(final boolean useNamedParameters) {
        this.useNamedParameters = useNamedParameters;
    }

    @Override
    public void serialize(final Parameters value, final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException {
        gen.writeObject(useNamedParameters ? value.named() : value.positional());
    }
}
