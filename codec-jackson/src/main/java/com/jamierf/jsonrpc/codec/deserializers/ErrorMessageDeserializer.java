package com.jamierf.jsonrpc.codec.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.jamierf.jsonrpc.api.ErrorMessage;
import com.jamierf.jsonrpc.util.Jackson;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class ErrorMessageDeserializer extends JsonDeserializer<ErrorMessage> {
    @Override
    public ErrorMessage deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
        final ObjectCodec codec = checkNotNull(jp.getCodec());
        final JsonNode node = codec.readTree(jp);

        return new ErrorMessage(
                Jackson.getInt(node, "code"),
                Jackson.getText(node, "message")
        );
    }
}
