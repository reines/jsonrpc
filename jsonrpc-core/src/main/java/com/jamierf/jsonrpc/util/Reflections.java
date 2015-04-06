package com.jamierf.jsonrpc.util;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class Reflections {

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

    public static boolean isVoid(final Class<?> type) {
        return Void.class.isAssignableFrom(type) || void.class.isAssignableFrom(type);
    }

    private Reflections() {
    }
}
