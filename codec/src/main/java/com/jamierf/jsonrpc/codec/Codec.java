package com.jamierf.jsonrpc.codec;

import com.jamierf.jsonrpc.util.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Codec {
    void writeValue(final OutputStream out, final Object message) throws IOException;
    <T> T readValue(final InputStream in, final TypeReference<T> type) throws IOException;
}
