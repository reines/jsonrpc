package com.jamierf.jsonrpc.transport;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;

import java.io.IOException;

public interface Transport {
    interface MessageListener {
        void onMessageInput(final ByteSource source) throws IOException;
    }

    void addListener(final MessageListener listener);

    ByteSink getMessageOutput() throws IOException;
}
