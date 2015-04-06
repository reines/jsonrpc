package com.jamierf.jsonrpc.codec.deserializers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.jamierf.jsonrpc.api.JsonRpcMessage;
import com.jamierf.jsonrpc.api.JsonRpcRequest;
import com.jamierf.jsonrpc.api.JsonRpcResponse;
import com.jamierf.jsonrpc.codec.JsonRpcModule;
import com.jamierf.jsonrpc.util.Nodes;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class JsonRpcMessageDeserializer extends JsonDeserializer<JsonRpcMessage> {
    @Override
    public JsonRpcMessage deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
        final ObjectCodec codec = checkNotNull(jp.getCodec());
        final JsonNode node = codec.readTree(jp);

        final String protocol = Nodes.getText(node, "jsonrpc");
        checkArgument(JsonRpcModule.PROTOCOL_VERSION.equals(protocol), "Unsupported protocol: " + protocol);

        // This must be a request
        if (node.has("method")) {
            return codec.readValue(codec.treeAsTokens(node), JsonRpcRequest.class);
        }

        // This must be a response
        if (node.has("result") || node.has("error")) {
            return codec.readValue(codec.treeAsTokens(node), JsonRpcResponse.class);
        }

        // This is an invalid message
        throw new JsonParseException("Invalid JSON-RPC message.", jp.getCurrentLocation());
    }
}
