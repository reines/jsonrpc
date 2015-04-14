package com.jamierf.jsonrpc.codec.fastjson.serializers;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.api.Result;

import java.io.IOException;
import java.lang.reflect.Type;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkArgument;

public class JsonRpcResponseSerializer implements ObjectSerializer {

    private final MetricRegistry metrics;

    public JsonRpcResponseSerializer(final MetricRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public void write(final JSONSerializer serializer, final Object object, final Object fieldName,
                      final Type fieldType, final int features) throws IOException {
        final JsonRpcResponse<?> value = (JsonRpcResponse<?>) object;
        final SerializeWriter out = serializer.getWriter();

        final Timer.Context timer = metrics.timer(name(JsonRpcResponseSerializer.class, "serialize")).time();
        try {
            out.write('{');
            out.writeFieldName("jsonrpc");
            serializer.write(JsonRpcMessage.PROTOCOL_VERSION);

            if (value.getId() != null) {
                out.writeFieldValue(',', "id", value.getId());
            }

            final Optional<? extends Result<?>> result = value.getResult();
            final Optional<? extends ErrorMessage> error = value.getError();
            checkArgument(result.isPresent() ^ error.isPresent(), "Only one of result and error may be present");

            if (error.isPresent()) {
                out.write(',');
                out.writeFieldName("error");
                serializer.write(error.get());
            }

            if (result.isPresent()) {
                out.write(',');
                out.writeFieldName("result");
                serializer.write(result.get().get());
            }

            out.write('}');
        } finally {
            timer.stop();
        }
    }
}
