package com.jamierf.jsonrpc.codec;

import com.jamierf.jsonrpc.codec.jackson.JacksonCodecFactory;

public class JacksonJsonRpcDeserializersTest extends JsonRpcDeserializersTest {
    public JacksonJsonRpcDeserializersTest() {
        super (new JacksonCodecFactory());
    }
}
