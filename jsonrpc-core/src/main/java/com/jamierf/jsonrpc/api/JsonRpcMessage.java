package com.jamierf.jsonrpc.api;

import com.google.common.base.MoreObjects;

public class JsonRpcMessage {

    private final String id;

    public JsonRpcMessage(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
