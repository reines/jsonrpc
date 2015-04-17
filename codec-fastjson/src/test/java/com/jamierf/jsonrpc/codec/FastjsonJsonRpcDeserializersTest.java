package com.jamierf.jsonrpc.codec;

import com.jamierf.jsonrpc.codec.fastjson.FastjsonCodecFactory;

public class FastjsonJsonRpcDeserializersTest extends JsonRpcDeserializersTest {
    public FastjsonJsonRpcDeserializersTest() {
        super (new FastjsonCodecFactory());
    }
}
