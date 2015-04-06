package com.jamierf.jsonrpc;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.net.HostAndPort;
import com.jamierf.jsonrpc.transport.AbstractTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketTransport extends AbstractTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketTransport.class);

    private final int maxFrameSize;
    private final Channel channel;

    public SocketTransport(final HostAndPort address, final int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;

        channel = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel channel) {
                        channel.pipeline()
                                .addLast(new JsonObjectDecoder(maxFrameSize, false)) // Part of Netty 5
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(final ChannelHandlerContext ctx, final Object msg)
                                            throws IOException {
                                        final ByteBuf buffer = ((ByteBuf) msg);
                                        putMessageInput(new ByteSource() {
                                            @Override
                                            public InputStream openStream() throws IOException {
                                                if (LOGGER.isTraceEnabled()) {
                                                    LOGGER.trace("<- {}", buffer.toString(StandardCharsets.UTF_8));
                                                }
                                                return new ByteBufferBackedInputStream(buffer.nioBuffer());
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
                final AtomicBoolean closed = new AtomicBoolean(false);
                return new ByteArrayOutputStream(maxFrameSize) {
                    @Override
                    public void close() throws IOException {
                        if (closed.compareAndSet(false, true)) {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("-> {}", new String(buf, 0, count, StandardCharsets.UTF_8));
                            }
                            channel.writeAndFlush(Unpooled.wrappedBuffer(buf, 0, count));
                            super.close();
                        }
                    }
                };
            }
        };
    }
}
