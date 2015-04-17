package com.jamierf.jsonrpc.codec.fastjson;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.google.common.io.CharStreams;
import com.jamierf.jsonrpc.api.*;
import com.jamierf.jsonrpc.codec.Codec;
import com.jamierf.jsonrpc.codec.fastjson.deserializers.ErrorMessageDeserializer;
import com.jamierf.jsonrpc.codec.fastjson.deserializers.JsonRpcMessageDeserializer;
import com.jamierf.jsonrpc.codec.fastjson.serializers.ErrorMessageSerializer;
import com.jamierf.jsonrpc.codec.fastjson.serializers.JsonRpcRequestSerializer;
import com.jamierf.jsonrpc.codec.fastjson.serializers.JsonRpcResponseSerializer;
import com.jamierf.jsonrpc.util.TypeReference;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FastjsonCodec implements Codec {

    private final ParserConfig parserConfig;
    private final SerializeConfig serializerConfig;

    public FastjsonCodec(final boolean useNamedParameters,
                         final Function<String, TypeReference<?>> responseTypeMapper,
                         final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper,
                         final MetricRegistry metrics) {
        parserConfig = new ParserConfig();
        parserConfig.putDeserializer(ErrorMessage.class, new ErrorMessageDeserializer());
        parserConfig.putDeserializer(JsonRpcMessage.class, new JsonRpcMessageDeserializer(requestParamTypeMapper, responseTypeMapper, metrics));

        serializerConfig = new SerializeConfig();
        serializerConfig.put(ErrorMessage.class, new ErrorMessageSerializer());
        serializerConfig.put(JsonRpcRequest.class, new JsonRpcRequestSerializer(useNamedParameters, metrics));
        serializerConfig.put(JsonRpcResponse.class, new JsonRpcResponseSerializer(metrics));
    }

    @Override
    public void writeValue(final OutputStream out, final Object message) throws IOException {
        try (final SerializeWriter writer = new SerializeWriter(new OutputStreamWriter(out))) {
            final JSONSerializer serializer = new JSONSerializer(writer, serializerConfig);
            serializer.write(message);
        }
    }

    @Override
    public <T> T readValue(final InputStream in, final TypeReference<T> type) throws IOException {
        final String json = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
        final DefaultJSONParser parser = new DefaultJSONParser(json, parserConfig);
        return parser.parseObject(type.getType());
    }
}
