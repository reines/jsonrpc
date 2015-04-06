package com.jamierf.jsonrpc.transport.socket;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;

import javax.net.ssl.SSLContext;

import static com.google.common.base.Preconditions.checkNotNull;

public class SocketTransportBuilder {

    private final HostAndPort address;

    private int maxFrameSize = 1024 * 1024; // 1Mb
    private Optional<SSLContext> sslContext = Optional.absent();

    protected SocketTransportBuilder(final HostAndPort address) {
        this.address = checkNotNull(address);
    }

    public SocketTransportBuilder maxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    public SocketTransportBuilder sslContext(final SSLContext sslContext) {
        this.sslContext = Optional.of(sslContext);
        return this;
    }

    public SocketTransport build() {
        return new SocketTransport(address, maxFrameSize, sslContext);
    }
}
