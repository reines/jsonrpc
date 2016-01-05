package com.jamierf.jsonrpc.transport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.verification.VerificationWithTimeout;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.jamierf.jsonrpc.util.SimpleLoggingRule;

public abstract class TransportTest<T extends Transport> {

	private static final long TIMEOUT = Duration.ofSeconds( 1 ).toMillis();
	private static final AtomicInteger MESSAGE_COUNTER = new AtomicInteger( 0 );

	@ClassRule
	public static SimpleLoggingRule logging = new SimpleLoggingRule().trace( "com.jamierf" );

	private static VerificationWithTimeout withTimeout() {
		return Mockito.timeout( TIMEOUT );
	}

	private static String generateJsonMessage() {
		return String.format( "{'text':'hello world %d'}", MESSAGE_COUNTER.getAndIncrement() );
	}

	protected abstract T createServer();

	protected abstract T createClient( final T server );

	private T server;
	private Transport.MessageListener serverListener;
	private T client;
	private Transport.MessageListener clientListener;

	@Before
	public void setUp() {
		server = createServer();
		serverListener = mock( Transport.MessageListener.class );
		server.addListener( serverListener );

		client = createClient( server );
		clientListener = mock( Transport.MessageListener.class );
		client.addListener( clientListener );
	}

	@Test
	public void testMultipleMessages() throws IOException {
		final List<String> expected = Lists.newLinkedList();
		for ( int i = 0; i < 10; i++ ) {
			final String message = generateJsonMessage();
			expected.add( message );
			client.getMessageOutput().write( message.getBytes() );
		}

		final ArgumentCaptor<ByteSource> captor = ArgumentCaptor.forClass( ByteSource.class );
		verify( serverListener, withTimeout().times( expected.size() ) ).onMessageInput(
				captor.capture(), any( ByteSink.class ) );

		final List<ByteSource> actual = captor.getAllValues();
		assertThat( actual, iterableWithSize( expected.size() ) );

		for ( int i = 0; i < expected.size(); i++ ) {
			final String message = new String( actual.get( i ).read() );
			assertThat( message, is( expected.get( i ) ) );
		}
	}

	@Test
	public void testMessageAndResponse() throws IOException, InterruptedException {
		client.getMessageOutput().write( generateJsonMessage().getBytes() );

		final ArgumentCaptor<ByteSink> sinkCaptor = ArgumentCaptor.forClass( ByteSink.class );
		verify( serverListener, withTimeout() ).onMessageInput( any( ByteSource.class ), sinkCaptor.capture() );

		sinkCaptor.getValue().write( generateJsonMessage().getBytes() );
		verify( clientListener, withTimeout() ).onMessageInput( any( ByteSource.class ), any( ByteSink.class ) );
	}

	@Test
	public void testMultipleClients() throws IOException {
		final int numClients = 10;

		for ( int i = 0; i < numClients; i++ ) {
			final T client = createClient( server );
			client.getMessageOutput().write( generateJsonMessage().getBytes() );
		}

		verify( serverListener, times( numClients ) ).onMessageInput( any( ByteSource.class ), any( ByteSink.class ) );
	}

	@After
	public void tearDown() {
		verifyNoMoreInteractions( serverListener, clientListener );

		client.close();
		server.close();
	}
}
