package com.jamierf.jsonrpc;

import static com.codahale.metrics.MetricRegistry.name;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.reflect.Reflection;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.codec.CodecFactory;
import com.jamierf.jsonrpc.error.CodedException;
import com.jamierf.jsonrpc.transport.Transport;

public class JsonRpcClient extends JsonRpcServer {

    private final Duration requestTimeout;
    private final MetricRegistry metrics;
    private final ScheduledExecutorService cleaner;

    protected JsonRpcClient(final Transport transport, final boolean useNamedParameters, final Duration requestTimeout,
                            final ExecutorService executor, final MetricRegistry metrics, final CodecFactory codecFactory,
                            final Supplier<Map<String, ?>> metadata) {
        super (transport, useNamedParameters, executor, metrics, codecFactory, metadata);

        this.requestTimeout = requestTimeout;
        this.metrics = metrics;

        cleaner = Executors.newSingleThreadScheduledExecutor();
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
        final JsonRpcRequest request = JsonRpcRequest.method(method, params, metadata.get());

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
                    pending.complete(new TimeoutException(String.format("Request timed out after %s", requestTimeout)));
                }
            }, requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        return pending.getFuture();
    }

    @SuppressWarnings("unchecked")
    protected <T> void handleResponse(final JsonRpcResponse<T> response) {
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

    @Override
    protected Optional<JsonRpcResponse<?>> handleMessage(JsonRpcMessage message) {
        if (message instanceof JsonRpcResponse<?>) {
            handleResponse((JsonRpcResponse<?>) message);
            return Optional.empty();
        }

        return super.handleMessage(message);
    }

    @Override
    public void close() {
        super.close();
        cleaner.shutdown();
    }
}
