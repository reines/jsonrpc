package com.jamierf.jsonrpc.codec.jackson;

import com.jamierf.jsonrpc.codec.JsonRpcDeserializersTest;

public class JacksonJsonRpcDeserializersTest extends JsonRpcDeserializersTest {
    public JacksonJsonRpcDeserializersTest() {
        super (new JacksonCodecFactory());
    }
}
