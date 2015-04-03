package com.jamierf.jsonrpc.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public final class Reflections {

    public static Map<String, ?> parameterMap(final Method method, final Object[] values) {
        if (values == null) {
            return Collections.emptyMap();
        }

        final Parameter[] keys = method.getParameters();
        checkArgument(values.length == keys.length, "Invalid number of parameters");
        final Map<String, Object> result = Maps.newHashMap(); // Need to accept nulls

        for (int index = 0; index < keys.length; index++) {
            result.put(keys[index].getName(), values[index]);
        }

        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Type> parameterTypeMap(final Method method) {
        final ImmutableMap.Builder<String, Type> params = ImmutableMap.builder();

        for (final Parameter param : method.getParameters()) {
            params.put(param.getName(), param.getParameterizedType());
        }

        return params.build();
    }

    public static ParameterizedType parameterizedType(final Class<?> type, final Class<?>... argumentTypes) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return argumentTypes;
            }

            @Override
            public Type getRawType() {
                return type;
            }

            @Override
            public Type getOwnerType() {
                return type;
            }
        };
    }

    public static <T> TypeReference<T> reference(final Type type) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return type;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> classOf(final Type type) {
        if (type instanceof ParameterizedType) {
            return classOf(((ParameterizedType) type).getRawType());
        }

        if (type instanceof Class<?>) {
            return (Class<T>) type;
        }

        throw new IllegalArgumentException("Unknown type: " + type);
    }

    private Reflections() {
    }
}
