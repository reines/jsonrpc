package com.jamierf.jsonrpc.codec;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Parameters;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import static com.jamierf.jsonrpc.api.ErrorMessageMatchers.withCode;
import static com.jamierf.jsonrpc.api.ErrorMessageMatchers.withMessage;
import static com.jamierf.jsonrpc.api.JsonRpcRequestMatchers.forMethod;
import static com.jamierf.jsonrpc.api.JsonRpcRequestMatchers.withParams;
import static com.jamierf.jsonrpc.api.JsonRpcResponseMatchers.withError;
import static com.jamierf.jsonrpc.api.JsonRpcResponseMatchers.withResult;
import static com.jamierf.jsonrpc.util.TypeReference.reference;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public abstract class JsonRpcDeserializersTest {

    @Rule
    public final SerializationTestRule namedSerialization;
    @Rule
    public final SerializationTestRule positionalSerialization;
    @Rule
    public final BenchmarkRule benchmark = new BenchmarkRule();

    public JsonRpcDeserializersTest(final CodecFactory codecFactory) {
        this.namedSerialization = new SerializationTestRule(true, codecFactory);
        this.positionalSerialization = new SerializationTestRule(false, codecFactory);
    }

    @Test
    public void testErrorResponse() throws IOException {
        namedSerialization.mockPendingResponse("1", reference(String.class));

        final JsonRpcResponse<?> result = (JsonRpcResponse<?>) namedSerialization.deserialize("error_response.json");

        assertThat(result, withError(allOf(
                withCode(500),
                withMessage("test error")
        )));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValidResponse() throws IOException {
        namedSerialization.mockPendingResponse("1", reference(String.class));

        final JsonRpcResponse<String> result = (JsonRpcResponse<String>) namedSerialization.deserialize("string_response.json");

        assertThat(result, withResult(
                equalTo("hello world")
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResponseToUnknownRequest() throws IOException {
        namedSerialization.deserialize("string_response.json");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValidComplexResponse() throws IOException {
        namedSerialization.mockPendingResponse("1", reference(TestEntity.class));

        final JsonRpcResponse<TestEntity> result = (JsonRpcResponse<TestEntity>) namedSerialization.deserialize("test_response.json");

        assertThat(result, withResult(
                equalTo(new TestEntity("hello world", 7))
        ));
    }

    @Test
    public void testNoParamsRequest() throws IOException {
        namedSerialization.mockMethod("ping");

        final JsonRpcRequest request = (JsonRpcRequest) namedSerialization.deserialize("no_params_request.json");

        assertThat(request, allOf(
                forMethod("ping"),
                withParams(Parameters.none())
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestForUnknownMethod() throws IOException {
        namedSerialization.deserialize("no_params_request.json");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestForMethodWithNotEnoughParams() throws IOException {
        namedSerialization.mockMethod("ping", Parameters.of(
                "a", reference(String.class)
        ));

        namedSerialization.deserialize("params_request.json");
    }

    @Test
    public void testParamsRequest() throws IOException {
        namedSerialization.mockMethod("ping", Parameters.of(
                "name", reference(String.class),
                "age", reference(int.class),
                "dob", reference(Date.class),
                "test", reference(TestEntity.class)
        ));

        final JsonRpcRequest request = (JsonRpcRequest) namedSerialization.deserialize("params_request.json");

        assertThat(request, allOf(
                forMethod("ping"),
                withParams(Parameters.of(
                        "name", "timmy",
                        "age", 3,
                        "dob", new Date(1427900421000L),
                        "test", new TestEntity("hello world", 7)
                ))
        ));
    }

    @Test
    public void testPositionalParamsRequest() throws IOException {
        namedSerialization.mockMethod("ping", Parameters.of(
                "name", reference(String.class),
                "age", reference(int.class),
                "dob", reference(Date.class),
                "test", reference(TestEntity.class)
        ));

        final JsonRpcRequest request = (JsonRpcRequest) namedSerialization.deserialize("positional_params_request.json");

        assertThat(request, allOf(
                forMethod("ping"),
                withParams(Parameters.of(
                        "name", "timmy",
                        "age", 3,
                        "dob", new Date(1427900421000L),
                        "test", new TestEntity("hello world", 7)
                ))
        ));
    }

    @Test
    public void testErrorMessageDeserialized() throws IOException {
        final ErrorMessage error = namedSerialization.deserialize("error_message.json", reference(ErrorMessage.class));

        assertThat(error, allOf(
                not(nullValue()),
                withCode(100),
                withMessage("test message")
        ));
    }
}
