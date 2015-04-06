package com.jamierf.jsonrpc.util;

import com.google.common.io.ByteSink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class ByteArraySink extends ByteSink {

    private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

    @Override
    public OutputStream openStream() throws IOException {
        final AtomicBoolean closed = new AtomicBoolean(false);
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                if (closed.compareAndSet(false, true)) {
                    writeTo(stream);
                    super.close();
                }
            }
        };
    }

    public byte[] toByteArray() {
        return stream.toByteArray();
    }
}
