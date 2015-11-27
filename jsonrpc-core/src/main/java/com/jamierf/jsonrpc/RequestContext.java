package com.jamierf.jsonrpc;

import java.util.Map;
import java.util.Optional;

import com.google.common.io.ByteSink;
import com.google.common.reflect.Reflection;

public final class RequestContext {

	private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();

	public synchronized static void set( final JsonRpcServer server, final Map<String, ?> metadata, final ByteSink output ) {
		context.set(new RequestContext(server, metadata, output));
	}

	public synchronized static Optional<RequestContext> get() {
		return Optional.ofNullable(context.get());
	}

	public synchronized static void clear() {
		context.remove();
	}

	private final JsonRpcServer server;
	private final Map<String, ?> metadata;
	private final ByteSink output;

	public RequestContext( final JsonRpcServer server, final Map<String, ?> metadata,
			final ByteSink output ) {
		this.server = server;
		this.metadata = metadata;
		this.output = output;
	}

	public <T> T proxy(final Class<T> remoteInterface) {
		return proxy(null, remoteInterface);
	}

	@SuppressWarnings("unchecked")
	public <T> T proxy(final String namespace, final Class<T> remoteInterface) {
		return Reflection
				.newProxy(remoteInterface, (proxy, method, args) -> server.call(namespace, method, args, output).get());
	}

	public <T> Optional<T> get( final String key, final Class<T> type ) {
		return Optional.ofNullable( metadata.get( key ) ).map( type::cast );
	}
}
