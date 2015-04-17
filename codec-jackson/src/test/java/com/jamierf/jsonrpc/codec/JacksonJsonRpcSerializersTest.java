package com.jamierf.jsonrpc.codec;

import com.jamierf.jsonrpc.codec.jackson.JacksonCodecFactory;

public class JacksonJsonRpcSerializersTest extends JsonRpcSerializersTest {
    public JacksonJsonRpcSerializersTest() {
        super (new JacksonCodecFactory());
    }
}
