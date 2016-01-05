package com.jamierf.jsonrpc;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.codahale.metrics.MetricRegistry;
import com.google.common.reflect.Reflection;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.jamierf.jsonrpc.codec.CodecFactory;
import com.jamierf.jsonrpc.filter.RequestHandler;
import com.jamierf.jsonrpc.transport.Transport;

public class JsonRpcClient extends JsonRpcServer {

    protected JsonRpcClient(final Transport transport, final boolean useNamedParameters, final Duration requestTimeout,
                            final ListeningExecutorService executor, final MetricRegistry metrics, final CodecFactory codecFactory,
                            final Supplier<Map<String, ?>> metadata, final List<RequestHandler> requestHandlerChain) {
        super (transport, useNamedParameters, requestTimeout, executor, metrics, codecFactory, metadata, requestHandlerChain);
    }

    public <T> T proxy(final Class<T> remoteInterface) {
        return proxy(null, remoteInterface);
    }

    @SuppressWarnings("unchecked")
    public <T> T proxy(final String namespace, final Class<T> remoteInterface) {
        return Reflection.newProxy(remoteInterface, (proxy, method, args) -> call(namespace, method, args, transport.getMessageOutput()).get());
    }
}
