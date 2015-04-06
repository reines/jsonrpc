package com.jamierf.jsonrpc.transport;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;

import java.io.IOException;
import java.util.Collection;

public abstract class AbstractTransport implements Transport {

    private final Collection<MessageListener> listeners = Lists.newCopyOnWriteArrayList();

    protected void putMessageInput(final ByteSource source) throws IOException {
        try {
            listeners.forEach(l -> {
                try {
                    l.onMessageInput(source);
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            });
        }
        catch (RuntimeException e) {
            Throwables.propagateIfPossible(e.getCause(), IOException.class);
        }
    }

    @Override
    public void addListener(final MessageListener listener) {
        listeners.add(listener);
    }
}
