package com.jamierf.jsonrpc.codec.jackson;

import com.jamierf.jsonrpc.codec.JsonRpcSerializersTest;

public class JacksonJsonRpcSerializersTest extends JsonRpcSerializersTest {
    public JacksonJsonRpcSerializersTest() {
        super (new JacksonCodecFactory());
    }
}
