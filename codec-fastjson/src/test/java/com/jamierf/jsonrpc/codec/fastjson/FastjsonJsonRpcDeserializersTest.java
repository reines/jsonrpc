package com.jamierf.jsonrpc.codec.fastjson;

import com.jamierf.jsonrpc.codec.JsonRpcDeserializersTest;

public class FastjsonJsonRpcDeserializersTest extends JsonRpcDeserializersTest {
    public FastjsonJsonRpcDeserializersTest() {
        super (new FastjsonCodecFactory());
    }
}
