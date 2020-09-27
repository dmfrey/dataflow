package com.vmware.dmfrey.dataflow;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestServer {

	private static final TestServer singleton = new TestServer();

	private volatile boolean shutdown;

	private static final int DEFAULT_PORT = 6666;

	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
	private final ExecutorService executorService = Executors.newCachedThreadPool();
	private final PropertyChangeSupport observable = new PropertyChangeSupport( this );
	private final Lock fairLock = new ReentrantLock(true) ;

	private static int selectedPort = DEFAULT_PORT;

	private TestServer() { }

	public static TestServer getInstance() {

		return singleton;
	}

	public static void main( String[] args ) {

		if( args.length != 0 ) {
			selectedPort = Integer.parseInt( args[ 0 ] );
		}

		ServerSocket serverSocket = null;

		try {

			serverSocket = new ServerSocket( selectedPort );

			TestServer.getInstance().registerMessageSender();
			TestServer.getInstance().registerClientConnectionCheck();

			System.out.println( "Waiting for connections..." );

			while( !TestServer.getInstance().isShutdown() ) {

				var clientSocket = serverSocket.accept();
				var in = new BufferedReader( new InputStreamReader( clientSocket.getInputStream() ) );
				var out = new PrintWriter( clientSocket.getOutputStream(), true );

				var clientHandler = new ClientHandler( clientSocket, in, out );
				TestServer.getInstance().startClient( clientHandler );

			}

		} catch( IOException e ) {

			if( null != serverSocket ) {

				try {

					serverSocket.close();

				} catch ( IOException e1 ) {

					e1.printStackTrace();

				}

			}

			e.printStackTrace();

		}

	}

	public void shutdown() {

		shutdown = true;

	}

	public boolean isShutdown() {

		return shutdown;
	}

	void unregisterClient( PropertyChangeListener observer ) {

		fairLock.lock();
		try {

			this.observable.removePropertyChangeListener( observer );

		} finally {

			fairLock.unlock();

		}

	}

	private void registerMessageSender() {

		this.scheduledExecutorService.scheduleAtFixedRate( new MessageSender(), 10, 5, TimeUnit.SECONDS );

		System.out.println( "registered message sender service" );
	}

	private void registerClientConnectionCheck() {

		this.scheduledExecutorService.scheduleAtFixedRate( new HeartbeatMonitor(), 1, 2, TimeUnit.SECONDS );

		System.out.println( "registered heartbeat service" );
	}

	private void registerClient( PropertyChangeListener observer ) {

		this.observable.addPropertyChangeListener( observer );

	}

	private void startClient( ClientHandler runnable ) {

		Future<?> task = this.executorService.submit( runnable );

	}

	private class HeartbeatMonitor implements Runnable {

		@Override
		public void run() {

			fairLock.lock();
			try {

				observable.firePropertyChange( "connection-check", null, null );

				System.out.printf( "heartbeat check initiated : %s%n", LocalDateTime.now() );

			} finally {

				fairLock.unlock();

			}

		}

	}

	private class MessageSender implements Runnable {

		@Override
		public void run() {

			fairLock.lock();
			try {

				observable.firePropertyChange( "payload-received", null, "message received" );

				System.out.printf( "payload initiated : %s%n", LocalDateTime.now() );

			} finally {

				fairLock.unlock();

			}

		}

	}

	private static class ClientHandler implements Runnable, PropertyChangeListener {

		static final long HEARTBEAT_CHECK = 12000;

		private Socket clientSocket;
		private BufferedReader in;
		private PrintWriter out;

		private String clientId;
		private LocalDateTime lastHeartbeat;
		private boolean shutdown;

		public ClientHandler( Socket clientSocket, BufferedReader in, PrintWriter out ) {

			this.clientSocket = clientSocket;
			this.in = in;
			this.out = out;

		}

		@Override
		public void run() {

			try {

				String received;
				while( !TestServer.getInstance().isShutdown() && ( received = in.readLine() ) != null ) {
					System.out.printf( "Server Status: %s, Thread Status: %s%n", ( TestServer.getInstance().isShutdown() ? "DOWN" : "UP" ), ( isShutdown() ? "DOWN" : "UP" ) );
					if( isShutdown() ) {

						break;
					}

					if( !received.contains( ":" ) ) {
						System.err.println( "unrecognized message format" );

						out.println( "unrecognized message format" );

					}

					String[] message = received.split( ":" );
					switch( message[ 0 ] ) {

						case "register" :

							register( message );
							break;

						case "heartbeat" :

							heartbeat( message );
							break;

						case "close" :

							close( message );
							break;
					}

				}

			} catch( IOException  e ) {

				e.printStackTrace();

			} finally {

				TestServer.getInstance().unregisterClient( this );

				System.out.printf( "closed thread: %s%n", clientId );

			}

			System.out.println( "Thread complete." );
		}

		void register( String[] message ) {

			System.out.printf( "registered client connection : %s%n", message[ 1 ] );

			clientId = message[ 1 ];
			lastHeartbeat = LocalDateTime.now();
			TestServer.getInstance().registerClient( this );

			out.println( String.format( "registered client connection : %s", message[ 1 ] ) );

		}

		void heartbeat( String[] message ) {

			System.out.printf( "client connection verified : %s%n", message[ 1 ] );

			lastHeartbeat = LocalDateTime.now();

			out.println( String.format( "client connection verified : %s", message[ 1 ] ) );

		}

		void close( String[] message ) {

			System.out.printf( "closing client connection : %s%n", message[ 1 ] );

			out.println( String.format( "closing client connection : %s", message[ 1 ] ) );

			shutdownClient();

		}

		void checkConnection() {

			var now = LocalDateTime.now();
			if( null == lastHeartbeat || Duration.between( lastHeartbeat, now ).toMillis() > HEARTBEAT_CHECK ) {

				System.out.printf( "heartbeat check failed, closing client connection : %s%n", clientId );

				out.println( String.format( "heartbeat check failed, closing client connection : %s%n", clientId ) );

				shutdownClient();

			}

		}

		void shutdownClient() {

			System.out.printf( "shutdown client connection : %s%n", clientId );

			lastHeartbeat = null;
			shutdown = true;

		}

		boolean isShutdown() {

			return shutdown;
		}

		void sendMessage( String message ) {

			out.println( String.format( "payload:%s:%s", clientId, message ) );

		}

		@Override
		public void propertyChange( PropertyChangeEvent evt ) {

			if( null == clientId || isShutdown() ) {
				return;
			}

			System.out.printf( "client %s : received broadcast...%n", clientId );

			String commandName = evt.getPropertyName();
			switch( commandName ) {

				case "connection-check" :

					checkConnection();
					break;

				case "payload-received" :

					sendMessage( (String) evt.getNewValue() );
					break;

			}

		}

	}

}
