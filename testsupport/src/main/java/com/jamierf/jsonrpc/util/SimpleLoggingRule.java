package com.jamierf.jsonrpc.util;

import static org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY;
import static org.slf4j.impl.SimpleLogger.LOG_KEY_PREFIX;

import java.util.Optional;
import java.util.Set;

import org.junit.rules.ExternalResource;

import com.google.common.collect.Sets;

public class SimpleLoggingRule extends ExternalResource {

	enum LogLevel {
		ALL,
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR
	}

	private final Optional<LogLevel> defaultLogLevel;
	private final Set<String> types = Sets.newHashSet();

	public SimpleLoggingRule() {
		this.defaultLogLevel = Optional.empty();
	}

	public SimpleLoggingRule( final LogLevel defaultLogLevel ) {
		this.defaultLogLevel = Optional.of( defaultLogLevel );
	}

	public SimpleLoggingRule set( final Class<?> type, final LogLevel logLevel ) {
		return set( type.getName(), logLevel );
	}

	public SimpleLoggingRule set( final String type, final LogLevel logLevel ) {
		types.add( type );
		System.setProperty( LOG_KEY_PREFIX + type, logLevel.toString() );
		return this;
	}

	public SimpleLoggingRule all( final Class<?> type ) {
		return set( type, LogLevel.ALL );
	}

	public SimpleLoggingRule all( final String type ) {
		return set( type, LogLevel.ALL );
	}

	public SimpleLoggingRule trace( final Class<?> type ) {
		return set( type, LogLevel.TRACE );
	}

	public SimpleLoggingRule trace( final String type ) {
		return set( type, LogLevel.TRACE );
	}

	public SimpleLoggingRule debug( final Class<?> type ) {
		return set( type, LogLevel.DEBUG );
	}

	public SimpleLoggingRule debug( final String type ) {
		return set( type, LogLevel.DEBUG );
	}

	public SimpleLoggingRule info( final Class<?> type ) {
		return set( type, LogLevel.INFO );
	}

	public SimpleLoggingRule info( final String type ) {
		return set( type, LogLevel.INFO );
	}

	public SimpleLoggingRule warn( final Class<?> type ) {
		return set( type, LogLevel.WARN );
	}

	public SimpleLoggingRule warn( final String type ) {
		return set( type, LogLevel.WARN );
	}

	public SimpleLoggingRule error( final Class<?> type ) {
		return set( type, LogLevel.ERROR );
	}

	public SimpleLoggingRule error( final String type ) {
		return set( type, LogLevel.ERROR );
	}

	@Override
	protected void before() {
		if ( defaultLogLevel.isPresent() ) {
			System.setProperty( DEFAULT_LOG_LEVEL_KEY, defaultLogLevel.get().toString() );
		}
	}

	@Override
	protected void after() {
		if ( defaultLogLevel.isPresent() ) {
			System.clearProperty( DEFAULT_LOG_LEVEL_KEY );
		}

		for ( final String type : types ) {
			System.clearProperty( LOG_KEY_PREFIX + type );
		}
	}
}
