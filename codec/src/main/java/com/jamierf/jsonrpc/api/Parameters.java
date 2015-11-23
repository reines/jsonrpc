package com.jamierf.jsonrpc.api;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.jamierf.jsonrpc.util.TypeReference;

public class Parameters<K, V> {

    @SuppressWarnings("unchecked")
    public static <K, V> Parameters<K, V> of(final Object... entries) {
        checkArgument(entries.length % 2 == 0, "Unmatched number of entries");

        final Builder<K, V> parameters = Parameters.builder();

        for (int index = 0; index < entries.length;) {
            final Object key = entries[index++];
            final Object value = entries[index++];

            parameters.add((K) key, (V) value);
        }

        return parameters.build();
    }

    public static Parameters<String, TypeReference<?>> typeReference(final Parameter[] keys) {
        final Builder<String, TypeReference<?>> parameters = Parameters.builder();

        for (final Parameter param : keys) {
            parameters.add(param.getName(), new TypeReference<Object>() {
                @Override
                public Type getType() {
                    return param.getParameterizedType();
                }
            });
        }

        return parameters.build();
    }

    public static <V> Parameters<String, V> zip(final Parameter[] keys, final V[] values) {
        if (values == null) {
            return Parameters.none();
        }

        checkArgument(values.length == keys.length, "Invalid number of parameters");
        final Builder<String, V> parameters = Parameters.builder();

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
            params.put(name, Optional.ofNullable(value));
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
        return Optional.ofNullable(params.get(key))
                .map(p -> p.orElse(null));
    }

    public Iterator<Map.Entry<K, V>> iterator() {
        return Iterators.unmodifiableIterator(named().entrySet().iterator());
    }

    public Map<K, V> named() {
        return Maps.transformValues(params, p -> p.orElse(null));
    }

    public Set<K> keys() {
        return Collections.unmodifiableSet(params.keySet());
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
