package com.jamierf.jsonrpc;

import com.jamierf.jsonrpc.codec.fastjson.FastjsonCodecFactory;
import org.junit.Ignore;

public class FastjsonRpcServerTest extends JsonRpcServerTest {
    public FastjsonRpcServerTest() {
        super (new FastjsonCodecFactory());
    }
}
