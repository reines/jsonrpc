package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jamierf.jsonrpc.PendingResponse;
import com.jamierf.jsonrpc.RequestMethod;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.util.Jackson;
import com.jamierf.jsonrpc.util.Reflections;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonRpcDeserializersTest {

    private static final JsonFactory JSON_FACTORY = Jackson.newObjectMapper().getFactory();

    private Map<String, PendingResponse<?>> requests;
    private Map<String, RequestMethod> methods;
    private JsonDeserializer<?> deserializer;
    private DeserializationContext ctxt;

    @Before
    public void setUp() throws JsonMappingException {
        requests = Maps.newHashMap();
        methods = Maps.newHashMap();

        deserializer = new JsonRpcDeserializers(
                Functions.forMap(Maps.transformValues(requests, PendingResponse::getType)),
                Functions.forMap(Maps.transformValues(methods, RequestMethod::getParameterTypes))
        ).findBeanDeserializer(SimpleType.construct(JsonRpcMessage.class), null, null);

        ctxt = mock(DeserializationContext.class);
    }

    private RequestMethod mockMethod(final String name) {
        return mockMethod(name, Collections.emptyMap());
    }

    private RequestMethod mockMethod(final String name, final Map<String, Type> types) {
        final RequestMethod requestMethod = mock(RequestMethod.class);
        when(requestMethod.getParameterTypes()).thenReturn(Maps.transformValues(types, Reflections::reference));

        methods.put(name, requestMethod);
        return requestMethod;
    }

    private Object deserialize(final String resource) throws IOException {
        final JsonParser jp = createParser(resource);
        return deserializer.deserialize(jp, ctxt);
    }

    @Test
    public void testErrorResponse() throws IOException {
        requests.put("1", PendingResponse.expectedType(String.class));

        final JsonRpcResponse<?> result = (JsonRpcResponse<?>) deserialize("error_response.json");

        assertThat(result, withError(allOf(
                withCode(500),
                withMessage("test error")
        )));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValidResponse() throws IOException {
        requests.put("1", PendingResponse.expectedType(String.class));

        final JsonRpcResponse<String> result = (JsonRpcResponse<String>) deserialize("string_response.json");

        assertThat(result, withResult(
                equalTo("hello world")
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResponseToUnknownRequest() throws IOException {
        deserialize("string_response.json");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValidComplexResponse() throws IOException {
        requests.put("1", PendingResponse.expectedType(TestEntity.class));

        final JsonRpcResponse<TestEntity> result = (JsonRpcResponse<TestEntity>) deserialize("test_response.json");

        assertThat(result, withResult(
                equalTo(new TestEntity("hello world", 7))
        ));
    }

    @Test
    public void testNoParamsRequest() throws IOException {
        mockMethod("ping");

        final JsonRpcRequest request = (JsonRpcRequest) deserialize("no_params_request.json");

        assertThat(request, allOf(
                forMethod("ping"),
                withParams(Collections.emptyMap())
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestForUnknownMethod() throws IOException {
        deserialize("no_params_request.json");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestForMethodWithNotEnoughParams() throws IOException {
        mockMethod("ping", ImmutableMap.of(
                "a", String.class
        ));

        deserialize("params_request.json");
    }

    @Test(expected = JsonProcessingException.class)
    public void testRequestForMethodWithWrongParamTypes() throws IOException {
        mockMethod("ping", ImmutableMap.of(
                "name", String.class,
                "age", int.class,
                "dob", Date.class,
                "test", String.class
        ));

        deserialize("params_request.json");
    }

    @Test
    public void testParamsRequest() throws IOException {
        mockMethod("ping", ImmutableMap.of(
                "name", String.class,
                "age", int.class,
                "dob", Date.class,
                "test", TestEntity.class
        ));

        final JsonRpcRequest request = (JsonRpcRequest) deserialize("params_request.json");
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

    private static JsonParser createParser(final String resource) throws IOException {
        return JSON_FACTORY.createParser(JsonRpcDeserializersTest.class.getResourceAsStream(resource));
    }
}
