package com.jamierf.jsonrpc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.MoreExecutors;
import com.jamierf.jsonrpc.codec.CodecFactory;
import com.jamierf.jsonrpc.transport.Transport;
import com.jamierf.jsonrpc.util.ByteArraySink;
import com.jamierf.jsonrpc.util.SimpleLoggingRule;

public abstract class JsonRpcServerTest {

    @ClassRule
    public static SimpleLoggingRule logging = new SimpleLoggingRule().trace( "com.jamierf" );

    public interface Interface {
        String ping();
        void testNoResponse();
    }

    @Rule
    public final BenchmarkRule benchmark = new BenchmarkRule();

    private final CodecFactory codecFactory;

    private ByteArraySink response;
    private JsonRpcServer server;

    public JsonRpcServerTest(final CodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    @Before
    public void setUp() throws IOException {
        response = new ByteArraySink();

        final Transport transport = mock(Transport.class);
        when(transport.getMessageOutput()).thenReturn(response);

        server = JsonRpc.server(transport, codecFactory)
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
        server.onMessage(byteResource("single_request_no_response.json"), response);
        assertThat(readResponse(), nullValue());
    }

    @Test
    public void testSingleRequestWithResponse() throws IOException {
        server.onMessage(byteResource("single_request.json"), response);
        assertThat(readResponse(), sameJSONAs(stringResource("single_response.json")));
    }

    @Test
    public void testBatchedRequestWithNoResponse() throws IOException {
        server.onMessage(byteResource("batched_request_no_response.json"), response);
        assertThat(readResponse(), nullValue());
    }

    @Test
    public void testBatchedRequestWithResponse() throws IOException {
        server.onMessage(byteResource("batched_request.json"), response);
        assertThat(readResponse(), sameJSONAs(stringResource("batched_response.json")).allowingAnyArrayOrdering());
    }

    @Test
    public void testMessagesAreLineDelimited() throws IOException {
        server.onMessage(byteResource("batched_request.json"), response);
        assertThat(readResponse(), endsWith(System.lineSeparator()));
    }

    private String readResponse() {
        final byte[] bytes = response.toByteArray();
        return bytes.length == 0 ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private static ByteSource byteResource(final String path) {
        return Resources.asByteSource(JsonRpcServer.class.getResource(path));
    }

    private static String stringResource(final String resource) throws IOException {
        return Resources.toString(JsonRpcServer.class.getResource(resource), StandardCharsets.UTF_8);
    }
}
