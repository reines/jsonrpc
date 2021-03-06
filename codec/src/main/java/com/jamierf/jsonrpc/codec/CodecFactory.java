package com.jamierf.jsonrpc.codec;

import java.util.ServiceLoader;
import java.util.function.Function;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Iterables;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.util.TypeReference;

public interface CodecFactory {

    static Iterable<CodecFactory> load() {
        return Iterables.unmodifiableIterable(ServiceLoader.load(CodecFactory.class));
    }

    Codec create(final boolean useNamedParameters,
                 final Function<String, TypeReference<?>> responseTypeMapper,
                 final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper,
                 final MetricRegistry metrics);
}
