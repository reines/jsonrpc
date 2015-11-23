package com.jamierf.jsonrpc;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.Futures;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Result;
import com.jamierf.jsonrpc.codec.Codec;
import com.jamierf.jsonrpc.codec.CodecFactory;
import com.jamierf.jsonrpc.transport.Transport;
import com.jamierf.jsonrpc.util.TypeReference;

public class JsonRpcServer {

    private static final byte[] DELIMITER = System.lineSeparator().getBytes(StandardCharsets.UTF_8);
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRpcServer.class);

    protected final Transport transport;
    protected final MetricRegistry metrics;
    protected final Codec codec;
    protected final Map<String, PendingResponse<?>> requests;
    protected final Map<String, RequestMethod> methods;
    protected final ExecutorService executor;
    protected final Supplier<Map<String, ?>> metadata;

    protected JsonRpcServer(final Transport transport, final boolean useNamedParameters, final ExecutorService executor,
                            final MetricRegistry metrics, final CodecFactory codecFactory,
                            final Supplier<Map<String, ?>> metadata) {
        this.transport = transport;
        this.metrics = metrics;
        this.executor = executor;
        this.metadata = metadata;

        requests = Maps.newConcurrentMap();
        methods = Maps.newConcurrentMap();

        codec = codecFactory.create(useNamedParameters,
                Maps.transformValues(requests, PendingResponse::getType)::get,
                Maps.transformValues(methods, RequestMethod::getParameterTypes)::get,
                metrics);

        transport.addListener(this::onMessage);
    }

    public <T> void register(final T instance, final Class<T> type) {
        register(null, instance, type);
    }

    public <T> void register(final String namespace, final T instance, final Class<T> type) {
        for (final Method method : type.getMethods()) {
            methods.put(zipNamespace(namespace, method.getName()), new RequestMethod(method, instance));
        }
    }

    protected void send(final Object message) {
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

    protected Optional<JsonRpcResponse<?>> handleRequest(final JsonRpcRequest request) {
        final Timer.Context timer = metrics.timer(name("api", request.getMethod())).time();
        try {
            final RequestMethod method = methods.get(request.getMethod());
            if (method == null) {
                return Optional.of(request.error(ErrorMessage.CODE_METHOD_NOT_FOUND,
                        "No such method: " + request.getMethod(), metadata.get()));
            }

            RequestContext.putAll(request.getMetadata());
            final Optional<Result<?>> result = method.invoke(request.getParams());
            RequestContext.clear();

            return result.map(r -> request.response(r, metadata.get()));
        } catch (Exception e) {
            LOGGER.warn("Error handling request " + request.getId(), e);
            return Optional.of(request.error(ErrorMessage.CODE_INTERNAL_ERROR, e.getMessage(), metadata.get()));
        } finally {
            timer.stop();
        }
    }

    protected Optional<JsonRpcResponse<?>> handleMessage(final JsonRpcMessage message) {
        if (message instanceof JsonRpcRequest) {
            return handleRequest((JsonRpcRequest) message);
        }

        return Optional.of(JsonRpcResponse.error(
                ErrorMessage.CODE_INTERNAL_ERROR, "Unknown message type: " + message, metadata.get()));
    }

    protected Collection<JsonRpcMessage> readMessage(final ByteSource input) throws IOException {
        final Timer.Context timer = metrics.timer(name(JsonRpcServer.class, "read-message")).time();
        try (final InputStream in = input.openStream()) {
            return codec.readValue(in, new TypeReference<Collection<JsonRpcMessage>>() {});
        } finally {
            timer.stop();
        }
    }

    protected void onMessage(final ByteSource input) throws IOException {
        final Collection<JsonRpcMessage> messages = readMessage(input);
        final boolean batched = messages.size() > 1;

        // Submit all messages for handling
        final Collection<Future<Optional<JsonRpcResponse<?>>>> futures = FluentIterable.from(messages)
                .transform(m -> executor.submit(() -> handleMessage(m)))
                .toList();

        // Combine all responses and filter out requests that don't require a response
        final Collection<JsonRpcResponse<?>> responses = FluentIterable.from(futures)
                .transform(Futures::getUnchecked)
                .filter(Optional::isPresent)
                .transform(Optional::get)
                .toList();

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

    public void close() {
        transport.close();
    }

    protected static String zipNamespace(final String... parts) {
        return Joiner.on('.').skipNulls().join(parts);
    }
}
