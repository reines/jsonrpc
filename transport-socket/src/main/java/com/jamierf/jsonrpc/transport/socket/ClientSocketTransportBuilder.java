package com.jamierf.jsonrpc.transport.socket;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.net.HostAndPort;

public class ClientSocketTransportBuilder {

    private final HostAndPort address;

    private int maxFrameSize = 1024 * 1024; // 1Mb

    protected ClientSocketTransportBuilder(final HostAndPort address) {
        this.address = checkNotNull(address);
    }

    public ClientSocketTransportBuilder maxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    public SocketTransport build() {
        return new SocketTransport(address, maxFrameSize);
    }
}
