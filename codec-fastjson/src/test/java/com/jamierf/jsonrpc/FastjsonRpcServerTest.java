package com.jamierf.jsonrpc;

import com.jamierf.jsonrpc.codec.fastjson.FastjsonCodecFactory;

public class FastjsonRpcServerTest extends JsonRpcServerTest {
    public FastjsonRpcServerTest() {
        super (new FastjsonCodecFactory());
    }
}
