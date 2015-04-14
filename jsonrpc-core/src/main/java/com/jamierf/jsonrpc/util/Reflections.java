package com.jamierf.jsonrpc.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class Reflections {

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

    public static boolean isVoid(final Class<?> type) {
        return Void.class.isAssignableFrom(type) || void.class.isAssignableFrom(type);
    }

    private Reflections() {
    }
}
