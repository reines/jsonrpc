package com.jamierf.jsonrpc.transport.socket;

import static com.google.common.base.Preconditions.checkState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.net.HostAndPort;
import com.jamierf.jsonrpc.transport.AbstractTransport;
import com.jamierf.jsonrpc.util.ByteBufferBackedInputStream;
import com.jamierf.jsonrpc.util.JsonObjectDecoder;

public class SocketTransport extends AbstractTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketTransport.class);

    public static ClientSocketTransportBuilder forClient(final String host, final int port) {
        return forClient(HostAndPort.fromParts(host, port));
    }

    public static ClientSocketTransportBuilder forClient(final HostAndPort address) {
        return new ClientSocketTransportBuilder(address);
    }

    public static ServerSocketTransportBuilder forServer(final int port) {
        return new ServerSocketTransportBuilder(port);
    }

    private final int maxFrameSize;
    private final Channel channel;

    protected SocketTransport(final int port, final int maxFrameSize, final Optional<SSLContext> sslContext) {
        this.maxFrameSize = maxFrameSize;

        channel = new ServerBootstrap()
            .group(new NioEventLoopGroup())
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .childHandler(createChannelHandler(sslContext))
            .bind(port)
            .syncUninterruptibly()
            .channel();
        LOGGER.info("Bound to: {}", port);
    }

    protected SocketTransport(final HostAndPort address, final int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;

        channel = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(createChannelHandler(Optional.empty()))
                .connect(address.getHostText(), address.getPort())
                .syncUninterruptibly()
                .channel();
        LOGGER.info("Connected to: {}", address);
    }

    protected ChannelHandler createChannelHandler(final Optional<SSLContext> sslContext) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel channel) {
                channel.pipeline()
                        .addLast("decoder", new JsonObjectDecoder(maxFrameSize, false))
                        .addLast("handler", new ChannelInboundHandlerAdapter() {
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

                if (sslContext.isPresent()) {
                    channel.pipeline().addLast("ssl", new SslHandler(sslContext.get().createSSLEngine()));
                }
            }
        };
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
                        checkState(closed.compareAndSet(false, true), "Stream already closed");

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("-> {}", new String(buf, 0, count, StandardCharsets.UTF_8));
                        }
                        channel.writeAndFlush(Unpooled.wrappedBuffer(buf, 0, count));
                        super.close();
                    }
                };
            }
        };
    }

    @Override
    public void close() {
        channel.close().syncUninterruptibly();
    }
}
