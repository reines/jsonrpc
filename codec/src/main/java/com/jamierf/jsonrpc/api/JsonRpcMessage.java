package com.jamierf.jsonrpc.api;

import java.util.Map;

import com.google.common.base.MoreObjects;

public class JsonRpcMessage {

    public static final String PROTOCOL_VERSION = "2.0";

    private final String id;
    private final Map<String, ?> metadata;

    public JsonRpcMessage(final String id, final Map<String, ?> metadata) {
        this.id = id;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public Map<String, ?> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("metadata", metadata)
                .toString();
    }
}
