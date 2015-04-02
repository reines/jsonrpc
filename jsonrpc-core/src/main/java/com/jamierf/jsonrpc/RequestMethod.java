package com.jamierf.jsonrpc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.jamierf.jsonrpc.util.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Map;

import static com.jamierf.jsonrpc.util.Reflections.parameterTypeMap;

public class RequestMethod {

    private final Method method;
    private final Object instance;
    private final Map<String, TypeReference<Object>> parameterTypes;

    public RequestMethod(final Method method, final Object instance) {
        this.method = method;
        this.instance = instance;

        parameterTypes = Maps.transformValues(parameterTypeMap(method), Reflections::reference);
    }

    public Map<String, TypeReference<?>> getParameterTypes() {
        return Collections.unmodifiableMap(parameterTypes);
    }

    private Object[] extractParameters(final Map<String, ?> params) {
        final Parameter[] keys = method.getParameters();
        final Object[] values = new Object[keys.length];

        for (int index = 0; index < values.length; index++) {
            values[index] = params.get(keys[index].getName());
        }

        return values;
    }

    public Optional<?> invoke(final Map<String, ?> params) {
        try {
            final Object result = method.invoke(instance, extractParameters(params));
            if (!Void.class.isAssignableFrom(method.getReturnType())) {
                return Optional.of(result);
            }

            return Optional.absent();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw Throwables.propagate(e);
        }
    }
}
