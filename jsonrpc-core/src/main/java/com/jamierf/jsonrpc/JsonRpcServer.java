package com.jamierf.jsonrpc;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.reflect.Reflection;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.jamierf.jsonrpc.api.*;
import com.jamierf.jsonrpc.codec.JsonRpcModule;
import com.jamierf.jsonrpc.error.CodedException;
import com.jamierf.jsonrpc.transport.Transport;
import com.jamierf.jsonrpc.util.Jackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.codahale.metrics.MetricRegistry.name;

public class JsonRpcServer {

    private static final byte[] DELIMITER = System.lineSeparator().getBytes(StandardCharsets.UTF_8);
    private static final String DEFAULT_METRIC_REGISTRY_NAME = "jsonrpc";
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRpcServer.class);

    private final Transport transport;
    private final MetricRegistry metrics;
    private final ObjectMapper codec;
    private final Map<String, PendingResponse<?>> requests;
    private final Map<String, RequestMethod> methods;
    private final ScheduledExecutorService cleaner;

    private final long requestTimeout;

    public JsonRpcServer(final Transport transport, final boolean useNamedParameters, final long requestTimeout) {
        this(transport, useNamedParameters, requestTimeout, SharedMetricRegistries.getOrCreate(DEFAULT_METRIC_REGISTRY_NAME));
    }

    public JsonRpcServer(final Transport transport, final boolean useNamedParameters, final long requestTimeout, final MetricRegistry metrics) {
        this.transport = transport;
        this.metrics = metrics;
        this.requestTimeout = requestTimeout;

        requests = Maps.newHashMap(); // TODO: expire requests after a reasonable (configurable?) duration
        methods = Maps.newHashMap();
        cleaner = Executors.newSingleThreadScheduledExecutor();

        codec = Jackson.newObjectMapper();
        codec.disable(SerializationFeature.CLOSE_CLOSEABLE);
        codec.registerModule(new JsonRpcModule(useNamedParameters,
                Functions.forMap(Maps.transformValues(requests, PendingResponse::getType)),
                Functions.forMap(Maps.transformValues(methods, RequestMethod::getParameterTypes))
        ));

        transport.addListener(this::onMessage);
    }

    @SuppressWarnings("unchecked")
    public <T> T proxy(final String namespace, final Class<T> remoteInterface) {
        return Reflection.newProxy(remoteInterface, (proxy, method, args) -> call(namespace, method, args).get());
    }

    protected <T> ListenableFuture<T> call(final String namespace, final Method method, final Object[] params) {
        return call(zipNamespace(namespace, method.getName()),
                Parameters.zip(method.getParameters(), params),
                method.getGenericReturnType());
    }

    protected <T> ListenableFuture<T> call(final String method, final Parameters<String, ?> params, final Type returnType) {
        final JsonRpcRequest request = JsonRpcRequest.method(method, params);

        final PendingResponse<T> pending = new PendingResponse<>(returnType);
        if (pending.expectsResponse()) {
            // Add the pending request to the request map, with a hook to remove it on completion
            requests.put(request.getId(), pending);
            pending.getFuture().addListener(() -> requests.remove(request.getId()), MoreExecutors.directExecutor());
        }

        send(request);

        if (pending.expectsResponse()) {
            // Add a scheduled task to timeout this request if we haven't received a response
            cleaner.schedule(() -> {
                if (!pending.isComplete()) {
                    pending.complete(new TimeoutException(String.format("Request timed out after %d ms", requestTimeout)));
                }
            }, requestTimeout, TimeUnit.MILLISECONDS);
        }

        return pending.getFuture();
    }

    public <T> void register(final T instance, final Class<T> type) {
        register(null, instance, type);
    }

    public <T> void register(final String namespace, final T instance, final Class<T> type) {
        for (final Method method : type.getMethods()) {
            methods.put(zipNamespace(namespace, method.getName()), new RequestMethod(method, instance));
        }
    }

    private void send(final Object message) {
        final Timer.Context timer = metrics.timer(name(JsonRpcServer.class, "send-message")).time();
        try (final OutputStream out = transport.getMessageOutput().openStream()) {
            codec.writeValue(out, message);
            out.write(DELIMITER);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            timer.stop();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void handleResponse(final JsonRpcResponse<T> response) {
        final Timer.Context timer = metrics.timer(name(JsonRpcServer.class, "process-response")).time();
        try {
            final PendingResponse<T> pending = (PendingResponse<T>) requests.get(response.getId());
            if (pending == null) {
                final Optional<ErrorMessage> error = response.getError();
                if (error.isPresent()) {
                    throw CodedException.fromErrorMessage(error.get());
                }

                throw new IllegalStateException("Received response to unknown request: " + response.getId());
            }

            pending.complete(response);
        } finally {
            timer.stop();
        }
    }

    private Optional<JsonRpcResponse<?>> handleRequest(final JsonRpcRequest request) {
        final Timer.Context timer = metrics.timer(name(JsonRpcServer.class, "process-request")).time();
        try {
            final RequestMethod method = methods.get(request.getMethod());
            if (method == null) {
                throw new IllegalArgumentException("No such method: " + request.getMethod());
            }

            final Optional<?> result = method.invoke(request.getParams());
            return result.transform(request::response);
        } catch (Exception e) {
            LOGGER.warn("Error handling request " + request.getId(), e);
            return Optional.of(request.error(100, e.getMessage())); // TODO
        } finally {
            timer.stop();
        }
    }

    private Optional<JsonRpcResponse<?>> handleMessage(final JsonRpcMessage message) {
        if (message instanceof JsonRpcResponse<?>) {
            handleResponse((JsonRpcResponse<?>) message);
            return Optional.absent();
        } else if (message instanceof JsonRpcRequest) {
            return handleRequest((JsonRpcRequest) message);
        }

        throw new IllegalArgumentException("Received unknown message type: " + message);
    }

    private Collection<JsonRpcMessage> readMessage(final ByteSource input) throws IOException {
        final Timer.Context timer = metrics.timer(name(JsonRpcServer.class, "read-message")).time();
        try (final InputStream in = input.openStream()) {
            return codec.readValue(in, new TypeReference<Collection<JsonRpcMessage>>() {});
        } finally {
            timer.stop();
        }
    }

    // TODO: Threading
    protected void onMessage(final ByteSource input) throws IOException {
        final Collection<JsonRpcMessage> messages = readMessage(input);
        final Collection<JsonRpcMessage> responses = Lists.newLinkedList();
        final boolean batched = messages.size() > 1;

        for (final JsonRpcMessage message : messages) {
            final Optional<JsonRpcResponse<?>> response = handleMessage(message);
            if (response.isPresent()) {
                responses.add(response.get());
            }
        }

        if (responses.isEmpty()) {
            return;
        }

        if (batched) {
           // There were more than 1 incoming, so it was a batch
            send(responses);
        } else {
            // Not a batch, so send each response individually
            responses.forEach(this::send);
        }
    }

    private static String zipNamespace(final String... parts) {
        return Joiner.on('.').skipNulls().join(parts);
    }
}
