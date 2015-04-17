package com.jamierf.jsonrpc.codec;

import com.jamierf.jsonrpc.codec.fastjson.FastjsonCodecFactory;

public class FastjsonJsonRpcSerializersTest extends JsonRpcSerializersTest {
    public FastjsonJsonRpcSerializersTest() {
        super (new FastjsonCodecFactory());
    }
}
