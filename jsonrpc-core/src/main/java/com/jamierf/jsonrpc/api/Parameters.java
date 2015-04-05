package com.jamierf.jsonrpc.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

import java.lang.reflect.Parameter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.jamierf.jsonrpc.util.Reflections.reference;

public class Parameters<K, V> {

    @SuppressWarnings("unchecked")
    public static <K, V> Parameters<K, V> of(final Object... entries) {
        checkArgument(entries.length % 2 == 0, "Unmatched number of entries");

        final Parameters.Builder<K, V> parameters = Parameters.builder();

        for (int index = 0; index < entries.length;) {
            final Object key = entries[index++];
            final Object value = entries[index++];

            parameters.add((K) key, (V) value);
        }

        return parameters.build();
    }

    public static Parameters<String, TypeReference<?>> typeReference(final Parameter[] keys) {
        final Parameters.Builder<String, TypeReference<?>> parameters = Parameters.builder();

        for (final Parameter param : keys) {
            parameters.add(param.getName(), reference(param.getParameterizedType()));
        }

        return parameters.build();
    }

    public static <V> Parameters<String, V> zip(final Parameter[] keys, final V[] values) {
        if (values == null) {
            return Parameters.none();
        }

        checkArgument(values.length == keys.length, "Invalid number of parameters");
        final Parameters.Builder<String, V> parameters = Parameters.builder();

        for (int index = 0; index < keys.length; index++) {
            parameters.add(keys[index].getName(), values[index]);
        }

        return parameters.build();
    }

    public static <K, V> Parameters<K, V> none() {
        return Parameters.<K, V>builder().build();
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public static class Builder<K, V> {

        private final LinkedHashMap<K, Optional<V>> params;

        private Builder() {
            params = Maps.newLinkedHashMap();
        }

        public Builder<K, V> add(final K name, final V value) {
            params.put(name, Optional.fromNullable(value));
            return this;
        }

        public Parameters<K, V> build() {
            return new Parameters<>(params);
        }
    }

    private final LinkedHashMap<K, Optional<V>> params;

    public Parameters(final LinkedHashMap<K, Optional<V>> params) {
        this.params = params;
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public Optional<V> get(final K key) {
        return Optional.fromNullable(params.get(key))
                .transform(Optional::orNull);
    }

    public Iterator<Map.Entry<K, V>> iterator() {
        return Iterators.unmodifiableIterator(named().entrySet().iterator());
    }

    public Map<K, V> named() {
        return Maps.transformValues(params, Optional::orNull);
    }

    @SuppressWarnings("unchecked")
    public V[] positional() {
        return (V[]) named().values().toArray();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameters<?, ?> that = (Parameters<?, ?>) o;
        return Objects.equal(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(params);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("params", named())
                .toString();
    }
}
