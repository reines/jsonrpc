package com.jamierf.jsonrpc.codec.fastjson.serializers;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.google.common.base.Optional;
import com.jamierf.jsonrpc.api.ErrorMessage;

import java.io.IOException;
import java.lang.reflect.Type;

public class ErrorMessageSerializer implements ObjectSerializer {
    @Override
    public void write(final JSONSerializer serializer, final Object object, final Object fieldName,
                      final Type fieldType, final int features) throws IOException {
        final ErrorMessage value = (ErrorMessage) object;
        final SerializeWriter out = serializer.getWriter();

        out.write('{');
        out.writeFieldName("code");
        serializer.write(value.getCode());

        out.writeFieldValue(',', "message", value.getMessage());

        final Optional<?> data = value.getData();
        if (data.isPresent()) {
            out.write(',');
            out.writeFieldName("data");
            serializer.write(data.get());
        }

        out.write('}');
    }
}
