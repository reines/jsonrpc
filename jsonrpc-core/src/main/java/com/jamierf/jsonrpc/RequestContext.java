package com.jamierf.jsonrpc;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;

public final class RequestContext {

	private static final ThreadLocal<Map<String, Object>> context = new ThreadLocal<Map<String, Object>>() {
		@Override
		protected Map<String, Object> initialValue() {
			return Maps.newHashMap();
		}
	};

	public static void put(final String key, final Object value) {
		context.get().put(key, value);
	}

	public static void putAll(final Map<String, ?> entries) {
		context.get().putAll(entries);
	}

	public static <T> Optional<T> get(final String key, final Class<T> type) {
		return Optional.ofNullable(context.get().get(key)).map(type::cast);
	}

	public static void clear() {
		context.remove();
	}

	private RequestContext() {}
}
