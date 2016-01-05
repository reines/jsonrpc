package com.jamierf.jsonrpc;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.api.Result;
import com.jamierf.jsonrpc.codec.Codec;
import com.jamierf.jsonrpc.codec.CodecFactory;
import com.jamierf.jsonrpc.error.CodedException;
import com.jamierf.jsonrpc.filter.RequestHandler;
import com.jamierf.jsonrpc.transport.Transport;
import com.jamierf.jsonrpc.util.TypeReference;

public class JsonRpcServer {

    private static final byte[] DELIMITER = System.lineSeparator().getBytes(StandardCharsets.UTF_8);
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRpcServer.class);

    protected final Transport transport;
    protected final Duration requestTimeout;
    protected final MetricRegistry metrics;
    protected final Codec codec;
    protected final Map<String, PendingResponse<?>> requests;
    protected final Map<String, RequestMethod> methods;
    protected final ListeningExecutorService executor;
    protected final Supplier<Map<String, ?>> metadata;
    protected final List<RequestHandler> requestHandlerChain;
    protected final ScheduledExecutorService cleaner;

    protected JsonRpcServer(final Transport transport, final boolean useNamedParameters, final Duration requestTimeout,
                            final ListeningExecutorService executor, final MetricRegistry metrics, final CodecFactory codecFactory,
                            final Supplier<Map<String, ?>> metadata, final List<RequestHandler> requestHandlerChain) {
        this.transport = transport;
        this.requestTimeout = requestTimeout;
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

        this.requestHandlerChain = Lists.newLinkedList(requestHandlerChain);
        this.requestHandlerChain.add(RequestMethod::invoke);

        cleaner = Executors.newSingleThreadScheduledExecutor();
    }

    public <T> void register(final T instance, final Class<T> type) {
        register(null, instance, type);
    }

    public <T> void register(final String namespace, final T instance, final Class<T> type) {
        for (final Method method : type.getMethods()) {
            methods.put(zipNamespace(namespace, method.getName()), new RequestMethod(namespace, method, instance));
        }
    }

    protected void send(final Object message, final ByteSink target) {
        final Timer.Context timer = metrics.timer(name(JsonRpcServer.class, "send-message")).time();
        try (final OutputStream out = target.openStream()) {
            codec.writeValue(out, message);
            out.write(DELIMITER);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            timer.stop();
        }
    }

    protected <T> ListenableFuture<T> call(final String namespace, final Method method, final Object[] params, final ByteSink output) {
        return call(zipNamespace(namespace, method.getName()),
            Parameters.zip(method.getParameters(), params),
            method.getGenericReturnType(), output);
    }

    protected <T> ListenableFuture<T> call(final String method, final Parameters<String, ?> params, final Type returnType, final ByteSink output) {
        final JsonRpcRequest request = JsonRpcRequest.method(method, params, metadata.get());

        final PendingResponse<T> pending = new PendingResponse<>(returnType);
        if (pending.expectsResponse()) {
            // Add the pending request to the request map, with a hook to remove it on completion
            requests.put(request.getId(), pending);
            pending.getFuture().addListener(() -> requests.remove(request.getId()), MoreExecutors.directExecutor());
        }

        send(request, output);

        if (pending.expectsResponse()) {
            // Add a scheduled task to timeout this request if we haven't received a response
			cleaner.schedule(() -> pending.complete(new TimeoutException(String.format(
					"Request timed out after %s", requestTimeout))), requestTimeout.toMillis(),
					TimeUnit.MILLISECONDS);
        }

        return pending.getFuture();
    }

    protected Optional<JsonRpcResponse<?>> handleRequest(final JsonRpcRequest request, final ByteSink output) {
        final Timer.Context timer = metrics.timer(name("api", request.getMethod())).time();
        try {
            final RequestMethod method = methods.get(request.getMethod());
            if (method == null) {
                return Optional.of(request.error(ErrorMessage.CODE_METHOD_NOT_FOUND,
                        "No such method: " + request.getMethod(), metadata.get()));
            }

            RequestContext.set(this, request.getMetadata(), output);

            // Return the response from the first handler in the chain that handles it
            final Optional<Result<?>> result = requestHandlerChain.stream()
                .map(h -> h.handle(method, request.getParams()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

            RequestContext.clear();

            return result.map(r -> request.response(r, metadata.get()));
        } catch (Exception e) {
            LOGGER.warn("Error handling request " + request.getId(), e);
            return Optional.of(request.error(ErrorMessage.CODE_INTERNAL_ERROR, e.getMessage(), metadata.get()));
        } finally {
            timer.stop();
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> void handleResponse(final JsonRpcResponse<T> response, final ByteSink output) {
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

    protected Optional<JsonRpcResponse<?>> handleMessage(final JsonRpcMessage message, final ByteSink output) {
        if (message instanceof JsonRpcRequest) {
            return handleRequest((JsonRpcRequest) message, output);
        }

        if (message instanceof JsonRpcResponse<?>) {
            handleResponse((JsonRpcResponse<?>) message, output);
            return Optional.empty();
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

    protected void onMessage(final ByteSource input, final ByteSink output) throws IOException {
        final Collection<JsonRpcMessage> messages = readMessage(input);
        final boolean batched = messages.size() > 1;

        // Submit all messages for handling
        final ListenableFuture<List<Optional<JsonRpcResponse<?>>>> future = Futures.allAsList(
            FluentIterable.from(messages)
                .transform(m -> executor.submit(() -> handleMessage(m, output)))
                .toList()
        );

        // Add a a listener to respond once the messages are handled
        future.addListener( () -> {
            // Combine all responses and filter out requests that don't require a response
            final Collection<JsonRpcResponse<?>> responses = FluentIterable.from(Futures.getUnchecked(future))
                .filter(Optional::isPresent)
                .transform(Optional::get)
                .toList();

            if (responses.isEmpty()) {
                return;
            }

            if (batched) {
                // There were more than 1 incoming, so it was a batch
                send(responses, output);
            } else {
                // Not a batch, so send each response individually
                for ( final JsonRpcResponse<?> response : responses ) {
                    send(response, output);
                }
            }
        }, MoreExecutors.directExecutor() );
    }

    public void close() {
        transport.close();
    }

    protected static String zipNamespace(final String... parts) {
        return Joiner.on('.').skipNulls().join(parts);
    }
}
