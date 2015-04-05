package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.google.common.base.Function;
import com.jamierf.jsonrpc.api.Parameters;

public class JsonRpcModule extends Module {

    public static final String PROTOCOL_VERSION = "2.0";

    private final boolean useNamedParameters;
    private final Function<String, TypeReference<?>> responseTypeMapper;
    private final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper;

    public JsonRpcModule(final boolean useNamedParameters,
                         final Function<String, TypeReference<?>> responseTypeMapper,
                         final Function<String, Parameters<String, TypeReference<?>>> requestParamTypeMapper) {
        this.useNamedParameters = useNamedParameters;
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
        context.addSerializers(new JsonRpcSerializers(useNamedParameters));
        context.addDeserializers(new JsonRpcDeserializers(responseTypeMapper, requestParamTypeMapper));
    }
}
