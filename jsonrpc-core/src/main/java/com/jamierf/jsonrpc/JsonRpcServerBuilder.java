package com.jamierf.jsonrpc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.collect.Lists;
import com.jamierf.jsonrpc.codec.CodecFactory;
import com.jamierf.jsonrpc.filter.RequestHandler;
import com.jamierf.jsonrpc.transport.Transport;

public class JsonRpcServerBuilder {
    private static final int DEFAULT_NUM_THREADS = 10;
    private static final String DEFAULT_METRIC_REGISTRY_NAME = "jsonrpc";

    private final Transport transport;
    private final CodecFactory codecFactory;
    private final List<RequestHandler> requestHandlerChain;

    private boolean useNamedParameters = true;
    private Duration requestTimeout = Duration.ofSeconds(1);
    private Optional<ExecutorService> executor = Optional.empty();
    private Optional<MetricRegistry> metrics = Optional.empty();
    private Supplier<Map<String, ?>> metadata = Collections::emptyMap;

    protected JsonRpcServerBuilder(final Transport transport, final CodecFactory codecFactory) {
        this.transport = checkNotNull(transport);
        this.codecFactory = checkNotNull(codecFactory);
        requestHandlerChain = Lists.newLinkedList();
    }

    public JsonRpcServerBuilder useNamedParameters(final boolean useNamedParameters) {
        this.useNamedParameters = useNamedParameters;
        return this;
    }

    public JsonRpcServerBuilder requestTimeout(final Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public JsonRpcServerBuilder executor(final ExecutorService executor) {
        this.executor = Optional.of(executor);
        return this;
    }

    public JsonRpcServerBuilder metrics(final MetricRegistry metrics) {
        this.metrics = Optional.of(metrics);
        return this;
    }

    public JsonRpcServerBuilder metadata(final Supplier<Map<String, ?>> metadata) {
        this.metadata = metadata;
        return this;
    }

    public JsonRpcServerBuilder filter(final RequestHandler requestHandler) {
        requestHandlerChain.add(requestHandler);
        return this;
    }

    public JsonRpcServer build() {
        return new JsonRpcServer(
                transport,
                useNamedParameters,
                requestTimeout,
                executor.orElseGet(() -> Executors.newFixedThreadPool(DEFAULT_NUM_THREADS)),
                metrics.orElseGet(() -> SharedMetricRegistries.getOrCreate(DEFAULT_METRIC_REGISTRY_NAME)),
                codecFactory,
                metadata,
                requestHandlerChain
        );
    }
}
