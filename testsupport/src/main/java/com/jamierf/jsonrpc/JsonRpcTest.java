package com.jamierf.jsonrpc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.google.common.util.concurrent.MoreExecutors;
import com.jamierf.jsonrpc.codec.CodecFactory;
import com.jamierf.jsonrpc.transport.Transport;
import com.jamierf.jsonrpc.util.SimpleLoggingRule;

public abstract class JsonRpcTest<T extends Transport> {

	@ClassRule
	public static SimpleLoggingRule logging = new SimpleLoggingRule().trace( "com.jamierf" );

	public interface TestApi {
		String ping();

		void testNoResponse();
	}

	private final CodecFactory codecFactory;

	private JsonRpcServer server;
	private JsonRpcClient client;
	private TestApi serverApi;
	private TestApi clientApi;

	public JsonRpcTest( final CodecFactory codecFactory ) {
		this.codecFactory = codecFactory;
	}

	protected abstract T createServer();
	protected abstract T createClient( final T server );

	@Before
	public void setUp() throws InterruptedException {
		final T serverTransport = createServer();
		server = JsonRpc.server( serverTransport, codecFactory )
				.executor( MoreExecutors.newDirectExecutorService() )
				.build();

		serverApi = spy( new TestApi() {
			@Override
			public String ping() {
				return "pong";
			}

			@Override
			public void testNoResponse() {
				/* no response */
			}
		} );

		server.register( serverApi, TestApi.class );

		final T clientTransport = createClient( serverTransport );
		client = JsonRpc.client( clientTransport, codecFactory )
				.requestTimeout( Duration.ofSeconds( 30 ) )
				.executor( MoreExecutors.newDirectExecutorService() )
				.build();

		clientApi = client.proxy( TestApi.class );
	}

	@Test
	public void testPing() {
		assertThat( clientApi.ping(), is( "pong" ) );
		verify( serverApi ).ping();
	}

	@After
	public void tearDown() {
		verifyNoMoreInteractions( serverApi );

		client.close();
		server.close();
	}
}
