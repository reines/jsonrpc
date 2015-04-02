package com.jamierf.jsonrpc;

import com.google.common.net.HostAndPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SocketJsonRpcServer extends JsonRpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketJsonRpcServer.class);
    private static final int MAX_FRAME_SIZE = 1024 * 1024 * 10; // 10Mb
    private static final String FRAME_TEMPLATE = "%s%n";

    private final Channel channel;

    public SocketJsonRpcServer(final HostAndPort address) {
        channel = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel channel) {
                        channel.pipeline()
                                .addLast(new StringEncoder(StandardCharsets.UTF_8) {
                                    @Override
                                    protected void encode(final ChannelHandlerContext ctx, final CharSequence msg,
                                                          final List<Object> out) throws Exception {
                                        super.encode(ctx, String.format(FRAME_TEMPLATE, msg), out);
                                    }
                                })
                                .addLast(new LineBasedFrameDecoder(MAX_FRAME_SIZE))
                                .addLast(new StringDecoder(StandardCharsets.UTF_8))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(final ChannelHandlerContext ctx, final Object msg)
                                            throws IOException {
                                        onMessage((String) msg);
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
    protected void send(final String string) throws IOException {
        channel.writeAndFlush(string);
    }
}
