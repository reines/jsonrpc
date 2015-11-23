package com.jamierf.jsonrpc.codec;

import static org.hamcrest.MatcherAssert.assertThat;

import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.google.common.io.Resources;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.Parameters;

public abstract class JsonRpcSerializersTest {

    private static final String MESSAGE_ID = "1";
    private static final Map<String, ?> METADATA = Collections.emptyMap();

    @Rule
    public final SerializationTestRule namedSerialization;
    @Rule
    public final SerializationTestRule positionalSerialization;
    @Rule
    public final BenchmarkRule benchmark = new BenchmarkRule();

    public JsonRpcSerializersTest(final CodecFactory codecFactory) {
        this.namedSerialization = new SerializationTestRule(true, codecFactory);
        this.positionalSerialization = new SerializationTestRule(false, codecFactory);
    }

    @Test
    public void testRequestWithoutParametersSerialized() throws IOException {
        final JsonRpcRequest request = new JsonRpcRequest("ping", Parameters.none(), MESSAGE_ID, METADATA);
        assertThat(namedSerialization.serialize(request), sameJSONAs(resource("no_params_request.json")));
    }

    @Test
    public void testRequestWithParametersSerialized() throws IOException {
        final JsonRpcRequest request = new JsonRpcRequest("ping", Parameters.of(
                "name", "timmy",
                "age", 3,
                "dob", new Date(1427900421000L),
                "test", new TestEntity("hello world", 7)
        ), MESSAGE_ID, METADATA);
        assertThat(namedSerialization.serialize(request), sameJSONAs(resource("params_request.json")));
    }

    @Test
    public void testRequestWithPositionalParametersSerialized() throws IOException {
        final JsonRpcRequest request = new JsonRpcRequest("ping", Parameters.of(
                "name", "timmy",
                "age", 3,
                "dob", new Date(1427900421000L),
                "test", new TestEntity("hello world", 7)
        ), MESSAGE_ID, METADATA);
        assertThat(positionalSerialization.serialize(request), sameJSONAs(resource("positional_params_request.json")));
    }

    private static String resource(final String resource) throws IOException {
        return Resources.toString(JsonRpcSerializersTest.class.getResource(resource), StandardCharsets.UTF_8);
    }
}
