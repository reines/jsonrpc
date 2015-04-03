package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import com.jamierf.jsonrpc.PendingResponse;
import com.jamierf.jsonrpc.RequestMethod;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.util.Jackson;
import com.jamierf.jsonrpc.util.Reflections;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import static com.jamierf.jsonrpc.util.Reflections.parameterizedType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SerializationTest extends ExternalResource {

    private final boolean useNamedParams;
    private ObjectMapper mapper;

    private Map<String, PendingResponse<?>> requests;
    private Map<String, RequestMethod> methods;

    public SerializationTest(final boolean useNamedParams) {
        this.useNamedParams = useNamedParams;
    }

    @Override
    protected void before() {
        requests = Maps.newHashMap();
        methods = Maps.newHashMap();

        mapper = Jackson.newObjectMapper();
        mapper.registerModule(new JsonRpcModule(useNamedParams,
                Functions.forMap(Maps.transformValues(requests, PendingResponse::getType)),
                Functions.forMap(Maps.transformValues(methods, RequestMethod::getParameterTypes))
        ));
    }

    public RequestMethod mockMethod(final String name) {
        return mockMethod(name, Collections.emptyMap());
    }

    public RequestMethod mockMethod(final String name, final Map<String, Type> types) {
        final RequestMethod requestMethod = mock(RequestMethod.class);
        when(requestMethod.getParameterTypes()).thenReturn(Maps.transformValues(types, Reflections::reference));

        methods.put(name, requestMethod);
        return requestMethod;
    }

    public <T> PendingResponse<T> mockPendingResponse(final String id, final Class<T> type, final Class<?>... argTypes) {
        final PendingResponse<T> pendingResponse = new PendingResponse<>(parameterizedType(type, argTypes));
        requests.put(id, pendingResponse);
        return pendingResponse;
    }

    public String serialize(final Object value) throws IOException {
        return mapper.writeValueAsString(value);
    }

    public JsonRpcMessage deserialize(final String resource) throws IOException {
        try (final InputStream in = JsonRpcDeserializersTest.class.getResourceAsStream(resource)) {
            return mapper.readValue(in, JsonRpcMessage.class);
        }
    }
}
