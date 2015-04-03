package com.jamierf.jsonrpc;

import com.google.common.base.Throwables;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import org.atmosphere.wasync.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class WebsocketJsonRpcServer extends JsonRpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketJsonRpcServer.class);

    private final Socket socket;

    public WebsocketJsonRpcServer(final String uri) throws IOException {
        LOGGER.info("Connecting to: {}", uri);
        final Client client = ClientFactory.getDefault().newClient();

        socket = client.create()
                .on(Event.MESSAGE, new Function<String>() {
                    @Override
                    public void on(final String string) {
                        try {
                            onMessage(new ByteSource() {
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
    protected ByteSink getOutput() {
        return new ByteSink() {
            @Override
            public OutputStream openStream() throws IOException {
                return new ByteArrayOutputStream() {
                    @Override
                    public void close() throws IOException {
                        socket.fire(new String(toByteArray(), StandardCharsets.UTF_8));
                        super.close();
                    }
                };
            }
        };
    }
}
