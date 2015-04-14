package com.jamierf.jsonrpc.codec.fastjson;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.codec.Codec;
import com.jamierf.jsonrpc.codec.CodecFactory;
import com.jamierf.jsonrpc.util.TypeReference;

public class FastjsonCodecFactory implements CodecFactory {
    @Override
    public Codec create(final boolean useNamedParameters, final Function<String, TypeReference<?>> responseTypeMapper,
                        final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper, final MetricRegistry metrics) {
        return new FastjsonCodec(useNamedParameters, responseTypeMapper, requestParamTypeMapper, metrics);
    }
}
