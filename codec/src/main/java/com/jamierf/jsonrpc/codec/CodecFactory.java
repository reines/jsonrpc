package com.jamierf.jsonrpc.codec;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.util.TypeReference;

import java.util.ServiceLoader;

public interface CodecFactory {

    static Iterable<CodecFactory> load() {
        return Iterables.unmodifiableIterable(ServiceLoader.load(CodecFactory.class));
    }

    Codec create(final boolean useNamedParameters,
                 final Function<String, TypeReference<?>> responseTypeMapper,
                 final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper,
                 final MetricRegistry metrics);
}
