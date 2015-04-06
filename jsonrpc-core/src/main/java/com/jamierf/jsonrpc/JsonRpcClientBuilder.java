package com.jamierf.jsonrpc;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.base.Optional;
import com.jamierf.jsonrpc.codec.CodecFactory;
import com.jamierf.jsonrpc.transport.Transport;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonRpcClientBuilder {
    private static final int DEFAULT_NUM_THREADS = 10;
    private static final String DEFAULT_METRIC_REGISTRY_NAME = "jsonrpc";

    private final Transport transport;
    private final CodecFactory codecFactory;

    private boolean useNamedParameters = true;
    private long requestTimeout = TimeUnit.SECONDS.toMillis(1);
    private Optional<ExecutorService> executor = Optional.absent();
    private Optional<MetricRegistry> metrics = Optional.absent();

    protected JsonRpcClientBuilder(final Transport transport, final CodecFactory codecFactory) {
        this.transport = checkNotNull(transport);
        this.codecFactory = checkNotNull(codecFactory);
    }

    public JsonRpcClientBuilder useNamedParameters(final boolean useNamedParameters) {
        this.useNamedParameters = useNamedParameters;
        return this;
    }

    public JsonRpcClientBuilder requestTimeout(final long requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public JsonRpcClientBuilder executor(final ExecutorService executor) {
        this.executor = Optional.of(executor);
        return this;
    }

    public JsonRpcClientBuilder metrics(final MetricRegistry metrics) {
        this.metrics = Optional.of(metrics);
        return this;
    }

    public JsonRpcClient build() {
        return new JsonRpcClient(
                transport,
                useNamedParameters,
                requestTimeout,
                executor.or(() -> Executors.newFixedThreadPool(DEFAULT_NUM_THREADS)),
                metrics.or(() -> SharedMetricRegistries.getOrCreate(DEFAULT_METRIC_REGISTRY_NAME)),
                codecFactory
        );
    }
}
