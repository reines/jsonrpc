package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.google.common.base.Function;

import java.util.Map;

public class JsonRpcModule extends Module {

    private final Function<String, TypeReference<?>> responseTypeMapper;
    private final Function<String, Map<String, TypeReference<?>>> requestParamTypeMapper;

    public JsonRpcModule(final Function<String, TypeReference<?>> responseTypeMapper,
                         final Function<String, Map<String, TypeReference<?>>> requestParamTypeMapper) {
        this.responseTypeMapper = responseTypeMapper;
        this.requestParamTypeMapper = requestParamTypeMapper;
    }

    @Override
    public String getModuleName() {
        return "jsonrpc";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(final SetupContext context) {
        context.addDeserializers(new JsonRpcDeserializers(responseTypeMapper, requestParamTypeMapper));
    }
}
