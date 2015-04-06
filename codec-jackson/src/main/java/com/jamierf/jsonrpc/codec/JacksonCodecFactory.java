package com.jamierf.jsonrpc.codec;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.util.TypeReference;

public class JacksonCodecFactory implements CodecFactory {
    @Override
    public Codec create(final boolean useNamedParameters,
                        final Function<String, TypeReference<?>> responseTypeMapper,
                        final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper,
                        final MetricRegistry metrics) {
        return new JacksonCodec(useNamedParameters, responseTypeMapper, requestParamTypeMapper, metrics);
    }
}
