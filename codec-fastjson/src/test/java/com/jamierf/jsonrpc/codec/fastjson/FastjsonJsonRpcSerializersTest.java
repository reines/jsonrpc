package com.jamierf.jsonrpc.codec.fastjson;

import com.jamierf.jsonrpc.codec.JsonRpcSerializersTest;

public class FastjsonJsonRpcSerializersTest extends JsonRpcSerializersTest {
    public FastjsonJsonRpcSerializersTest() {
        super (new FastjsonCodecFactory());
    }
}
