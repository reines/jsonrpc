package com.jamierf.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.reflect.Reflection;
import com.google.common.util.concurrent.ListenableFuture;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.codec.JsonRpcModule;
import com.jamierf.jsonrpc.error.CodedException;
import com.jamierf.jsonrpc.transport.Transport;
import com.jamierf.jsonrpc.util.Jackson;
import com.jamierf.jsonrpc.util.Reflections;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JsonRpcServer {

    private static final byte[] DELIMITER = System.lineSeparator().getBytes(StandardCharsets.UTF_8);

    private final Transport transport;
    private final ObjectMapper codec;
    private final Map<String, PendingResponse<?>> requests;
    private final Map<String, RequestMethod> methods;

    public JsonRpcServer(final Transport transport) {
        this.transport = transport;

        requests = Maps.newHashMap(); // TODO: expire requests after a reasonable (configurable?) duration
        methods = Maps.newHashMap();

        codec = Jackson.newObjectMapper();
        codec.registerModule(new JsonRpcModule(
                Functions.forMap(Maps.transformValues(requests, PendingResponse::getType)),
                Functions.forMap(Maps.transformValues(methods, RequestMethod::getParameterTypes))
        ));

        transport.addListener(this::onMessage);
    }

    @SuppressWarnings("unchecked")
    public <T> T proxy(final String namespace, final Class<T> remoteInterface) {
        return Reflection.newProxy(remoteInterface, (proxy, method, args) -> call(
                name(namespace, method.getName()),
                Reflections.parameterMap(method, args),
                method.getGenericReturnType()
        ).get());
    }

    protected <T> ListenableFuture<T> call(final String method, final Map<String, ?> params, final Type returnType) {
        final JsonRpcRequest request = JsonRpcRequest.method(method, params);

        final PendingResponse<T> pending = new PendingResponse<>(returnType);
        requests.put(request.getId(), pending);

        send(request);
        // TODO: Remove requests once their future completes

        return pending.getFuture();
    }

    public <T> void register(final String namespace, final T instance, final Class<T> type) {
        for (final Method method : type.getMethods()) {
            methods.put(name(namespace, method.getName()), new RequestMethod(method, instance));
        }
    }

    protected void send(final JsonRpcMessage message) {
        try (final OutputStream out = transport.getMessageOutput().openStream()) {
            codec.writeValue(out, message);
            out.write(DELIMITER);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void onResponse(final JsonRpcResponse<T> response) {
        final PendingResponse<T> pending = (PendingResponse<T>) requests.get(response.getId());
        if (pending == null) {
            final Optional<ErrorMessage> error = response.getError();
            if (error.isPresent()) {
                throw CodedException.fromErrorMessage(error.get());
            }

            throw new IllegalStateException("Received response to unknown request: " + response.getId());
        }

        pending.complete(response);
    }

    private void onRequest(final JsonRpcRequest request) {
        final RequestMethod method = methods.get(request.getMethod());
        if (method == null) {
            throw new IllegalArgumentException("No such method: " + request.getMethod());
        }

        try {
            final Optional<?> result = method.invoke(request.getParams());
            if (result.isPresent()) {
                final JsonRpcResponse<?> response = request.response(result.get());
                send(response);
            }
        } catch (Exception e) {
            final JsonRpcResponse<?> response = request.error(100, e.getMessage()); // TODO
            send(response);
        }
    }

    private JsonRpcMessage readMessage(final ByteSource input) throws IOException {
        try (final InputStream in = input.openStream()) {
            return codec.readValue(in, JsonRpcMessage.class);
        }
    }

    protected void onMessage(final ByteSource input) throws IOException {
        final JsonRpcMessage message = readMessage(input);
        if (message instanceof JsonRpcResponse<?>) {
            onResponse((JsonRpcResponse<?>) message);
        } else if (message instanceof JsonRpcRequest) {
            onRequest((JsonRpcRequest) message);
        }
    }

    private static String name(final String base, final String... parts) {
        return base + '.' + Joiner.on('.').join(parts);
    }
}
