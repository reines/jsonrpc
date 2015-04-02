package com.jamierf.jsonrpc;

import com.google.common.base.Throwables;
import org.atmosphere.wasync.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
                            onMessage(string);
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
    protected void send(final String string) throws IOException {
        socket.fire(string);
    }
}
