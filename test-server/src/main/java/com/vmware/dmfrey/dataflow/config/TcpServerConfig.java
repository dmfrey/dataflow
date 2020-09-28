package com.vmware.dmfrey.dataflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.tcp.connection.TcpConnectionCloseEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLfSerializer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Configuration
public class TcpServerConfig {

    private static final Logger log = LoggerFactory.getLogger( TcpServerConfig.class );
    private static final long HEARTBEAT_CHECK = 12000;

    private final Map<String, Client> clients = new ConcurrentHashMap();

    @Bean
    public TcpNetServerConnectionFactory connectionFactory( final TcpServerConfigurationProperties properties ) {

        TcpNetServerConnectionFactory connectionFactory = new TcpNetServerConnectionFactory( properties.getPort() );
        connectionFactory.setSerializer( new ByteArrayLfSerializer() );
        connectionFactory.setDeserializer( new ByteArrayLfSerializer() );

        return connectionFactory;
    }

    @Bean
    public IntegrationFlow serverIn( final TcpNetServerConnectionFactory connectionFactory ) {

        return IntegrationFlows.from( Tcp.inboundAdapter( connectionFactory ) )
                .log()
                .transform( Transformers.objectToString() )
                .split()
                    .publishSubscribeChannel( subscription ->
                        subscription
                                .subscribe( subflow -> subflow
                                        .filter( this::isRegisterMessage )
                                        .channel( "handleRegister.input" )
                                )
                                .subscribe( subflow -> subflow
                                        .filter( this::isHeartbeatMessage )
                                        .channel( "handleHeartbeat.input" )
                                )
                    )
                .get();
    }

    @Bean
    @Profile( "!test" )
    public IntegrationFlow serverOut() {

        return IntegrationFlows.from( () -> "seed", e -> e.poller( Pollers.fixedDelay(5, TimeUnit.SECONDS, 5 ) ) )
                .split( this.clients, "keySet" )
                .enrichHeaders( h -> h.headerExpression( IpHeaders.CONNECTION_ID, "payload" ) )
                .transform( p -> String.format( "payload:%s:message received, %s", p, LocalDateTime.now() ) )
                .log()
                .channel( "sendMessage.input" )
                .get();
    }

    @Bean
    public IntegrationFlow connectionCheck() {

        return IntegrationFlows.from( () -> "connection-check", e -> e.poller( Pollers.fixedDelay(5, TimeUnit.SECONDS, 1 ) ) )
                .split( this.clients, "keySet" )
                .enrichHeaders( h -> h.headerExpression( IpHeaders.CONNECTION_ID, "payload" ) )
                .handle( (p, h) -> {

                    var connectionId = (String) h.get( IpHeaders.CONNECTION_ID );
                    var client = clients.get( connectionId );
                    log.info( "Connection check for client {}", client );

                    if( null != client.getLastHeartbeat() ) {

                        return String.format( "closing client connection : %s", client.getClientId() );
                    }

                    var now = LocalDateTime.now();
                    if( Duration.between( client.getLastHeartbeat(), now ).toMillis() > HEARTBEAT_CHECK ) {

                        clients.remove( client.getClientId() );

                        return String.format( "heartbeat check failed, closing client connection : %s%n", client.getClientId() );
                    }

                    return String.format( "client connection verified : %s", client.getClientId() );
                })
                .log()
                .channel( "sendMessage.input" )
                .get();
    }

    @EventListener
    public void open( TcpConnectionOpenEvent event ) {
        log.debug( "open : enter, event={}", event );

        this.clients.put( event.getConnectionId(), new Client() );

        log.debug( "open : exit" );
    }

    @EventListener
    public void close( TcpConnectionCloseEvent event ) {
        log.debug( "close : enter, event={}", event );

        this.clients.remove( event.getConnectionId() );

        log.debug( "close : exit" );
    }

    private boolean isRegisterMessage( final String payload ) {
        log.debug( "isRegisterMessage : enter, payload={}", payload );

        return payload.startsWith( "register:" );
    }

    @Bean
    public IntegrationFlow handleRegister() {

        return f -> f
                .handle( (p, h) -> {
                    String[] message = ( (String) p ).split( ":" );

                    var connectionId = (String) h.get( IpHeaders.CONNECTION_ID );
                    clients.put( connectionId, new Client( message[ 1 ], LocalDateTime.now() ) );

                    log.info( "Registered new client {}", clients.get( connectionId ) );

                    return p;
                })
                .enrichHeaders( h -> h.headerExpression( IpHeaders.CONNECTION_ID, "payload" ) )
                .transform( p -> {
                    String[] message = ( (String) p ).split( ":" );

                    return String.format( "registered client connection : %s", message[ 1 ] );
                })
                .log()
                .channel( "sendMessage.input" );
    }

    private boolean isHeartbeatMessage( final String payload ) {
        log.debug( "isHeartbeatMessage : enter, payload={}", payload );

        return payload.startsWith( "heartbeat:" );
    }

    @Bean
    public IntegrationFlow handleHeartbeat( final TcpNetServerConnectionFactory connectionFactory ) {

        return f -> f
                .handle( (p, h) -> {

                    var connectionId = (String) h.get( IpHeaders.CONNECTION_ID );
                    clients.get( connectionId ).setLastHeartbeat( LocalDateTime.now() );

                    log.info( "Heartbeat received for client {}", clients.get( connectionId ) );

                    return p;
                })
                .enrichHeaders( h -> h.headerExpression( IpHeaders.CONNECTION_ID, "payload" ) )
                .transform( p -> {
                    String[] message = ( (String) p ).split( ":" );

                    return String.format( "client connection verified : %s", message[ 1 ] );
                })
                .log()
                .channel( "sendMessage.input" );
    }

    @Bean
    public IntegrationFlow sendMessage( final TcpNetServerConnectionFactory connectionFactory ) {

        return f -> f
                .log()
                .handle( Tcp.outboundAdapter( connectionFactory ) );
    }

}

class Client {

    private String clientId;
    private LocalDateTime lastHeartbeat;

    Client() { }

    Client( final String clientId, final LocalDateTime lastHeartbeat ) {

        this.clientId = clientId;
        this.lastHeartbeat = lastHeartbeat;
    }

    public String getClientId() {

        return clientId;
    }

    public void setClientId( final String clientId ) {

        this.clientId = clientId;

    }

    public LocalDateTime getLastHeartbeat() {

        return lastHeartbeat;
    }

    public void setLastHeartbeat( final LocalDateTime lastHeartbeat ) {

        this.lastHeartbeat = lastHeartbeat;

    }

    @Override
    public String toString() {
        return "Client{" +
                "clientId='" + clientId + '\'' +
                ", lastHeartbeat=" + lastHeartbeat +
                '}';
    }

}