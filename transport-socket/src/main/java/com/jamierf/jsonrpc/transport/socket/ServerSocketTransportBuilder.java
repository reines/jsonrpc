package com.jamierf.jsonrpc.transport.socket;

import java.util.Optional;

import javax.net.ssl.SSLContext;

public class ServerSocketTransportBuilder {

    private final int port;

    private int maxFrameSize = 1024 * 1024; // 1Mb
    private Optional<SSLContext> sslContext = Optional.empty();

    protected ServerSocketTransportBuilder(final int port) {
        this.port = port;
    }

    public ServerSocketTransportBuilder maxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    public ServerSocketTransportBuilder sslContext(final SSLContext sslContext) {
        this.sslContext = Optional.ofNullable(sslContext);
        return this;
    }

    public SocketTransport build() {
        return new SocketTransport(port, maxFrameSize, sslContext);
    }
}
