package com.jamierf.jsonrpc;

import com.jamierf.jsonrpc.codec.CodecFactory;
import com.jamierf.jsonrpc.transport.Transport;

public final class JsonRpc {

	public static JsonRpcClientBuilder client(final Transport transport, final CodecFactory codecFactory) {
		return new JsonRpcClientBuilder(transport, codecFactory);
	}

	public static JsonRpcServerBuilder server(final Transport transport, final CodecFactory codecFactory) {
		return new JsonRpcServerBuilder(transport, codecFactory);
	}

	private JsonRpc() {}
}
