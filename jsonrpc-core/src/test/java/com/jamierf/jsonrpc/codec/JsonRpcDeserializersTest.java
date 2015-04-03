package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static com.jamierf.jsonrpc.api.ErrorMessageMatchers.withCode;
import static com.jamierf.jsonrpc.api.ErrorMessageMatchers.withMessage;
import static com.jamierf.jsonrpc.api.JsonRpcRequestMatchers.forMethod;
import static com.jamierf.jsonrpc.api.JsonRpcRequestMatchers.withParams;
import static com.jamierf.jsonrpc.api.JsonRpcResponseMatchers.withError;
import static com.jamierf.jsonrpc.api.JsonRpcResponseMatchers.withResult;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class JsonRpcDeserializersTest {

    @Rule
    public SerializationTest serialization = new SerializationTest(true);

    @Test
    public void testErrorResponse() throws IOException {
        serialization.mockPendingResponse("1", String.class);

        final JsonRpcResponse<?> result = (JsonRpcResponse<?>) serialization.deserialize("error_response.json");

        assertThat(result, withError(allOf(
                withCode(500),
                withMessage("test error")
        )));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValidResponse() throws IOException {
        serialization.mockPendingResponse("1", String.class);

        final JsonRpcResponse<String> result = (JsonRpcResponse<String>) serialization.deserialize("string_response.json");

        assertThat(result, withResult(
                equalTo("hello world")
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResponseToUnknownRequest() throws IOException {
        serialization.deserialize("string_response.json");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValidComplexResponse() throws IOException {
        serialization.mockPendingResponse("1", TestEntity.class);

        final JsonRpcResponse<TestEntity> result = (JsonRpcResponse<TestEntity>) serialization.deserialize("test_response.json");

        assertThat(result, withResult(
                equalTo(new TestEntity("hello world", 7))
        ));
    }

    @Test
    public void testNoParamsRequest() throws IOException {
        serialization.mockMethod("ping");

        final JsonRpcRequest request = (JsonRpcRequest) serialization.deserialize("no_params_request.json");

        assertThat(request, allOf(
                forMethod("ping"),
                withParams(Collections.emptyMap())
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestForUnknownMethod() throws IOException {
        serialization.deserialize("no_params_request.json");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestForMethodWithNotEnoughParams() throws IOException {
        serialization.mockMethod("ping", ImmutableMap.of(
                "a", String.class
        ));

        serialization.deserialize("params_request.json");
    }

    @Test(expected = JsonProcessingException.class)
    public void testRequestForMethodWithWrongParamTypes() throws IOException {
        serialization.mockMethod("ping", ImmutableMap.of(
                "name", String.class,
                "age", int.class,
                "dob", Date.class,
                "test", String.class
        ));

        serialization.deserialize("params_request.json");
    }

    @Test
    public void testParamsRequest() throws IOException {
        serialization.mockMethod("ping", ImmutableMap.of(
                "name", String.class,
                "age", int.class,
                "dob", Date.class,
                "test", TestEntity.class
        ));

        final JsonRpcRequest request = (JsonRpcRequest) serialization.deserialize("params_request.json");
        final Map<String, ?> params = ImmutableMap.of(
                "name", "timmy",
                "age", 3,
                "dob", new Date(1427900421000L),
                "test", new TestEntity("hello world", 7)
        );

        assertThat(request, allOf(
                forMethod("ping"),
                withParams(params)
        ));
    }
}
