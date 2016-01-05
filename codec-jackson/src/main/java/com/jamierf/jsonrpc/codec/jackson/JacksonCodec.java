package com.jamierf.jsonrpc.codec.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.codec.Codec;
import com.jamierf.jsonrpc.util.Jackson;
import com.jamierf.jsonrpc.util.TypeReference;

public class JacksonCodec implements Codec {

    private final ObjectMapper codec;

    public JacksonCodec(final boolean useNamedParameters,
                        final Function<String, TypeReference<?>> responseTypeMapper,
                        final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper,
                        final MetricRegistry metrics) {
        codec = Jackson.newObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                .registerModule(new JsonRpcModule(useNamedParameters, responseTypeMapper, requestParamTypeMapper, metrics));
    }

    @Override
    public void writeValue(final OutputStream out, final Object message) throws IOException {
        codec.writeValue(out, message);
    }

    @Override
    public <T> T readValue(final InputStream in, final TypeReference<T> type) throws IOException {
        return codec.readValue(in, Jackson.reference(type));
    }
}
