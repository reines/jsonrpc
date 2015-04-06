package com.jamierf.jsonrpc;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.MoreExecutors;
import com.jamierf.jsonrpc.codec.jackson.JacksonCodecFactory;
import com.jamierf.jsonrpc.transport.Transport;
import com.jamierf.jsonrpc.util.ByteArraySink;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class JsonRpcClientTest {

    public interface Interface {
        String ping();
        void testNoResponse();
    }

    private ByteArraySink response;
    private JsonRpcClient server;

    @Rule
    public BenchmarkRule benchmark = new BenchmarkRule();

    @Before
    public void setUp() throws IOException {
        response = new ByteArraySink();

        final Transport transport = mock(Transport.class);
        when(transport.getMessageOutput()).thenReturn(response);

        server = JsonRpcClient.builder(transport, new JacksonCodecFactory())
                .executor(MoreExecutors.newDirectExecutorService())
                .build();

        server.register(new Interface() {
            @Override
            public String ping() {
                return "pong";
            }

            @Override
            public void testNoResponse() {
                /* no response */
            }
        }, Interface.class);
    }

    @Test
    public void testSingleRequestWithNoResponse() throws IOException {
        server.onMessage(byteResource("codec/single_request_no_response.json"));
        assertThat(readResponse(), nullValue());
    }

    @Test
    public void testSingleRequestWithResponse() throws IOException {
        server.onMessage(byteResource("codec/single_request.json"));
        assertThat(readResponse(), sameJSONAs(stringResource("codec/single_response.json")));
    }

    @Test
    public void testBatchedRequestWithNoResponse() throws IOException {
        server.onMessage(byteResource("codec/batched_request_no_response.json"));
        assertThat(readResponse(), nullValue());
    }

    @Test
    public void testBatchedRequestWithResponse() throws IOException {
        server.onMessage(byteResource("codec/batched_request.json"));
        assertThat(readResponse(), sameJSONAs(stringResource("codec/batched_response.json")));
    }

    @Test
    public void testMessagesAreLineDelimited() throws IOException {
        server.onMessage(byteResource("codec/batched_request.json"));
        assertThat(readResponse(), endsWith(System.lineSeparator()));
    }

    private String readResponse() {
        final byte[] bytes = response.toByteArray();
        return bytes.length == 0 ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private static ByteSource byteResource(final String path) {
        return Resources.asByteSource(JsonRpcClient.class.getResource(path));
    }

    private static String stringResource(final String resource) throws IOException {
        return Resources.toString(JsonRpcClient.class.getResource(resource), StandardCharsets.UTF_8);
    }
}
