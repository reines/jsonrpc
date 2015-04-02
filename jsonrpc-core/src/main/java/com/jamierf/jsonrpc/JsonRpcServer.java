package com.jamierf.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.reflect.Reflection;
import com.google.common.util.concurrent.ListenableFuture;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.codec.JsonRpcModule;
import com.jamierf.jsonrpc.util.Jackson;
import com.jamierf.jsonrpc.util.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

public abstract class JsonRpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRpcServer.class);

    private final ObjectMapper codec;
    private final Map<String, PendingResponse<?>> requests;
    private final Map<String, RequestMethod> methods;

    public JsonRpcServer() {
        requests = Maps.newHashMap();
        methods = Maps.newHashMap();

        codec = Jackson.newObjectMapper();
        codec.registerModule(new JsonRpcModule(
                Functions.forMap(Maps.transformValues(requests, PendingResponse::getType)),
                Functions.forMap(Maps.transformValues(methods, RequestMethod::getParameterTypes))
        ));
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
        try {
            final String string = codec.writeValueAsString(message);
            LOGGER.trace("-> {}", string);
            send(string);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    protected abstract void send(final String string) throws IOException;

    @SuppressWarnings("unchecked")
    private <T> void onResponse(final JsonRpcResponse<T> response) {
        final PendingResponse<T> pending = (PendingResponse<T>) requests.get(response.getId());
        if (pending == null) {
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

    protected void onMessage(final String string) throws IOException {
        LOGGER.trace("<- {}", string);
        final JsonRpcMessage message = codec.readValue(string, JsonRpcMessage.class);
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
