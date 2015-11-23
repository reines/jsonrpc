package com.jamierf.jsonrpc.codec;

import static com.jamierf.jsonrpc.util.TypeReference.reference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.rules.ExternalResource;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.util.TypeReference;

public class SerializationTestRule extends ExternalResource {

    private final boolean useNamedParams;
    private final CodecFactory codecFactory;

    private Map<String, TypeReference<?>> requests;
    private Map<String, Parameters<String, TypeReference<?>>> methods;
    private Codec mapper;

    public SerializationTestRule(final boolean useNamedParams, final CodecFactory codecFactory) {
        this.useNamedParams = useNamedParams;
        this.codecFactory = codecFactory;
    }

    @Override
    protected void before() {
        requests = Maps.newHashMap();
        methods = Maps.newHashMap();

        mapper = codecFactory.create(useNamedParams,
                requests::get,
                methods::get,
                new MetricRegistry()
        );
    }

    public void mockMethod(final String name) {
        mockMethod(name, Parameters.none());
    }

    public void mockMethod(final String name, final Parameters<String, TypeReference<?>> types) {
        methods.put(name, types);
    }

    public void mockPendingResponse(final String id, final TypeReference<?> type) {
        requests.put(id, type);
    }

    public String serialize(final Object value) throws IOException {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            mapper.writeValue(out, value);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            Throwables.propagateIfPossible(e.getCause());
            throw e;
        }
    }

    public <T> T deserialize(final String resource, final TypeReference<T> type) throws IOException {
        try (final InputStream in = JsonRpcDeserializersTest.class.getResourceAsStream(resource)) {
            return mapper.readValue(in, type);
        } catch (RuntimeException e) {
            Throwables.propagateIfPossible(e.getCause());
            throw e;
        }
    }

    public JsonRpcMessage deserialize(final String resource) throws IOException {
        return deserialize(resource, reference(JsonRpcMessage.class));
    }
}
