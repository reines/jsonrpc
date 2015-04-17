package com.jamierf.jsonrpc;

import com.jamierf.jsonrpc.codec.jackson.JacksonCodecFactory;

public class JacksonJsonRpcServerTest extends JsonRpcServerTest {
    public JacksonJsonRpcServerTest() {
        super (new JacksonCodecFactory());
    }
}
