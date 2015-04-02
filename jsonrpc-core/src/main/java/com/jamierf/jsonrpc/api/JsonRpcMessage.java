package com.jamierf.jsonrpc.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class JsonRpcMessage {

    private final String protocol;
    private final String id;

    public JsonRpcMessage(final String protocol, final String id) {
        this.protocol = protocol;
        this.id = id;
    }

    @JsonProperty("jsonrpc")
    public String getProtocol() {
        return protocol;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("protocol", protocol)
                .add("id", id)
                .toString();
    }
}
