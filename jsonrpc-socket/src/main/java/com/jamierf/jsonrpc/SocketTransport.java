package com.jamierf.jsonrpc;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.fasterxml.jackson.databind.util.ByteBufferBackedOutputStream;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.net.HostAndPort;
import com.jamierf.jsonrpc.transport.AbstractTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class SocketTransport extends AbstractTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketTransport.class);
    private static final int MAX_FRAME_SIZE = 1024 * 1024 * 1; // 1Mb

    private final Channel channel;

    public SocketTransport(final HostAndPort address) {
        channel = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel channel) {
                        channel.pipeline()
                                .addLast(new JsonBasedFrameDecoder(MAX_FRAME_SIZE))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(final ChannelHandlerContext ctx, final Object msg)
                                            throws IOException {
                                        final ByteBuffer buffer = ((ByteBuf) msg).nioBuffer();
                                        putMessageInput(new ByteSource() {
                                            @Override
                                            public InputStream openStream() throws IOException {
                                                return new ByteBufferBackedInputStream(buffer);
                                            }
                                        });
                                    }

                                    @Override
                                    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
                                            throws Exception {
                                        LOGGER.warn("Exception from channel: {}", ctx.channel(), cause);
                                    }
                                });
                    }
                })
                .connect(address.getHostText(), address.getPort())
                .syncUninterruptibly()
                .channel();
        LOGGER.info("Connected to: {}", address);
    }

    @Override
    public ByteSink getMessageOutput() {
        return new ByteSink() {
            @Override
            public OutputStream openStream() throws IOException {
                final ByteBuffer buffer = ByteBuffer.allocate(MAX_FRAME_SIZE);
                return new ByteBufferBackedOutputStream(buffer) {
                    @Override
                    public void close() throws IOException {
                        channel.writeAndFlush(buffer);
                        super.close();
                    }
                };
            }
        };
    }
}
