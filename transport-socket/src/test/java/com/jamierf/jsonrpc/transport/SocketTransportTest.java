package com.jamierf.jsonrpc.transport;

import com.jamierf.jsonrpc.transport.socket.SocketTransport;

public class SocketTransportTest extends TransportTest<SocketTransport> {
	@Override
	protected SocketTransport createServer() {
		return SocketTransport.forServer( 0 ).build();
	}

	@Override
	protected SocketTransport createClient( final SocketTransport server ) {
		return SocketTransport.forClient( "localhost", server.getLocalAddress().getPort() ).build();
	}
}
