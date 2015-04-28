package com.jamierf.jsonrpc;

import com.jamierf.jsonrpc.codec.fastjson.FastjsonCodecFactory;
import org.junit.Ignore;

@Ignore
public class FastjsonRpcServerTest extends JsonRpcServerTest {
    public FastjsonRpcServerTest() {
        super (new FastjsonCodecFactory());
    }
}
