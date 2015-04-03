package com.jamierf.jsonrpc.codec;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class JsonRpcSerializersTest {

    @Rule
    public SerializationTest serialization = new SerializationTest(true);

    @Test
    public void testRequestWithoutParametersSerialized() throws IOException {
        final JsonRpcRequest request = JsonRpcRequest.method("ping");
        assertThat(serialization.serialize(request), sameJSONAs(resource("no_params_request.json")));
    }

    @Test
    public void testRequestWithParametersSerialized() throws IOException {
        final JsonRpcRequest request = JsonRpcRequest.method("ping", ImmutableMap.of(
                "name", "timmy",
                "age", 3,
                "dob", new Date(1427900421000L),
                "test", new TestEntity("hello world", 7)
        ));
        assertThat(serialization.serialize(request), sameJSONAs(resource("params_request.json")));
    }

    private static String resource(final String resource) throws IOException {
        return Resources.toString(JsonRpcSerializersTest.class.getResource(resource), StandardCharsets.UTF_8);
    }
}
