package com.jamierf.jsonrpc.codec;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import com.jamierf.jsonrpc.PendingResponse;
import com.jamierf.jsonrpc.RequestMethod;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.codec.jackson.JacksonCodecFactory;
import com.jamierf.jsonrpc.util.TypeReference;
import org.junit.rules.ExternalResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.jamierf.jsonrpc.util.Reflections.parameterizedType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SerializationTestRule extends ExternalResource {

    private final boolean useNamedParams;
    private final CodecFactory codecFactory;

    private Map<String, PendingResponse<?>> requests;
    private Map<String, RequestMethod> methods;
    private Codec mapper;

    public SerializationTestRule(final boolean useNamedParams) {
        this (useNamedParams, new JacksonCodecFactory());
    }

    public SerializationTestRule(final boolean useNamedParams, final CodecFactory codecFactory) {
        this.useNamedParams = useNamedParams;
        this.codecFactory = codecFactory;
    }

    @Override
    protected void before() {
        requests = Maps.newHashMap();
        methods = Maps.newHashMap();

        mapper = codecFactory.create(useNamedParams,
                Functions.forMap(Maps.transformValues(requests, PendingResponse::getType)),
                Functions.forMap(Maps.transformValues(methods, RequestMethod::getParameterTypes)),
                new MetricRegistry()
        );
    }

    public RequestMethod mockMethod(final String name) {
        return mockMethod(name, Parameters.none());
    }

    public RequestMethod mockMethod(final String name, final Parameters<String, TypeReference<?>> types) {
        final RequestMethod requestMethod = mock(RequestMethod.class);
        when(requestMethod.getParameterTypes()).thenReturn(types);

        methods.put(name, requestMethod);
        return requestMethod;
    }

    public <T> PendingResponse<T> mockPendingResponse(final String id, final Class<T> type, final Class<?>... argTypes) {
        final PendingResponse<T> pendingResponse = new PendingResponse<>(parameterizedType(type, argTypes));
        requests.put(id, pendingResponse);
        return pendingResponse;
    }

    public String serialize(final JsonRpcMessage value) throws IOException {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            mapper.writeValue(out, value);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public JsonRpcMessage deserialize(final String resource) throws IOException {
        try (final InputStream in = JsonRpcDeserializersTest.class.getResourceAsStream(resource)) {
            return mapper.readValue(in, new TypeReference<JsonRpcMessage>() {});
        }
    }
}
