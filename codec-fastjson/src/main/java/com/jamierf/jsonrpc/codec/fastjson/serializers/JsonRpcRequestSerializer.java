package com.jamierf.jsonrpc.codec.fastjson.serializers;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;

import java.io.IOException;
import java.lang.reflect.Type;

import static com.codahale.metrics.MetricRegistry.name;

public class JsonRpcRequestSerializer implements ObjectSerializer {

    private final boolean useNamedParameters;
    private final MetricRegistry metrics;

    public JsonRpcRequestSerializer(final boolean useNamedParameters, final MetricRegistry metrics) {
        this.useNamedParameters = useNamedParameters;
        this.metrics = metrics;
    }

    @Override
    public void write(final JSONSerializer serializer, final Object object, final Object fieldName,
                      final Type fieldType, final int features) throws IOException {
        final JsonRpcRequest value = (JsonRpcRequest) object;
        final SerializeWriter out = serializer.getWriter();

        final Timer.Context timer = metrics.timer(name(JsonRpcRequestSerializer.class, "serialize")).time();
        try {
            out.write('{');
            out.writeFieldName("jsonrpc");
            serializer.write(JsonRpcMessage.PROTOCOL_VERSION);

            if (value.getId() != null) {
                out.writeFieldValue(',', "id", value.getId());
            }
            out.writeFieldValue(',', "method", value.getMethod());

            if (!value.getParams().isEmpty()) {
                out.write(',');
                out.writeFieldName("params");
                serializer.write(useNamedParameters ? value.getParams().named() : value.getParams().positional());
            }

            out.write('}');
        } finally {
            timer.stop();
        }
    }
}
