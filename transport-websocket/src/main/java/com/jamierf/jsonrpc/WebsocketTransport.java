package com.jamierf.jsonrpc;

import com.google.common.base.Throwables;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.jamierf.jsonrpc.transport.AbstractTransport;
import org.atmosphere.wasync.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebsocketTransport extends AbstractTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketTransport.class);

    private final Socket socket;

    public WebsocketTransport(final String uri) throws IOException {
        LOGGER.info("Connecting to: {}", uri);
        final Client client = ClientFactory.getDefault().newClient();

        socket = client.create()
                .on(Event.MESSAGE, new Function<String>() {
                    @Override
                    public void on(final String string) {
                        try {
                            putMessageInput(new ByteSource() {
                                @Override
                                public InputStream openStream() throws IOException {
                                    return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
                                }
                            });
                        } catch (IOException e) {
                            throw Throwables.propagate(e);
                        }
                    }
                })
                .on(Event.OPEN, o -> {
                    LOGGER.info("Connected to: {}", uri);
                })
                .on(Event.CLOSE, o -> {
                    LOGGER.info("Disconnected from: {}", uri);
                })
                .open(client.newRequestBuilder()
                                .method(Request.METHOD.GET)
                                .uri(uri)
                                .transport(Request.TRANSPORT.WEBSOCKET)
                                .build()
                );
    }

    @Override
    public ByteSink getMessageOutput() {
        return new ByteSink() {
            @Override
            public OutputStream openStream() throws IOException {
                final AtomicBoolean closed = new AtomicBoolean(false);
                return new ByteArrayOutputStream() {
                    @Override
                    public void close() throws IOException {
                        if (closed.compareAndSet(false, true)) {
                            socket.fire(new String(toByteArray(), StandardCharsets.UTF_8));
                            super.close();
                        }
                    }
                };
            }
        };
    }
}
