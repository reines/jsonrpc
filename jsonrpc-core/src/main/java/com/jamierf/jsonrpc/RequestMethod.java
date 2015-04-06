package com.jamierf.jsonrpc;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.jamierf.jsonrpc.api.Parameters;
import com.jamierf.jsonrpc.api.Result;
import com.jamierf.jsonrpc.util.TypeReference;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.jamierf.jsonrpc.util.Reflections.isVoid;

public class RequestMethod {

    private final Method method;
    private final Object instance;
    private final Parameters<String, TypeReference<?>> parameterTypes;

    public RequestMethod(final Method method, final Object instance) {
        this.method = method;
        this.instance = instance;

        parameterTypes = Parameters.typeReference(method.getParameters());
    }

    public Parameters<String, TypeReference<?>> getParameterTypes() {
        return parameterTypes;
    }

    public Optional<Result<?>> invoke(final Parameters params) {
        try {
            final Object result = method.invoke(instance, params.positional());
            if (!isVoid(method.getReturnType())) {
                return Optional.of(new Result<>(result));
            }

            return Optional.absent();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw Throwables.propagate(e);
        }
    }
}
