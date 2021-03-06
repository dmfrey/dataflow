package com.vmware.dmfrey.dataflow.tcpclient;

import com.vmware.dmfrey.dataflow.config.TcpServerConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLfSerializer;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;

@Configuration
public class TcpClient {

    private static final Logger log = LoggerFactory.getLogger( TcpClient.class );

    @Autowired
    private ClientManager clientManager;

    @Bean
    TcpNetClientConnectionFactory connectionFactory( final TcpServerConfigurationProperties properties ) {

        TcpNetClientConnectionFactory connectionFactory = new TcpNetClientConnectionFactory( properties.getUrl(), properties.getPort() );
        connectionFactory.setSerializer( new ByteArrayLfSerializer() );
        connectionFactory.setDeserializer( new ByteArrayLfSerializer() );

        return connectionFactory;
    }

    @Bean
    TcpSendingMessageHandler sendingMessageHandler( final TcpNetClientConnectionFactory connectionFactory ) {

        TcpSendingMessageHandler sendingMessageHandler = new TcpSendingMessageHandler();
        sendingMessageHandler.setConnectionFactory( connectionFactory );

        return sendingMessageHandler;
    }

    @Bean
    TcpReceivingChannelAdapter receivingChannelAdapter( final TcpNetClientConnectionFactory connectionFactory ) {

        TcpReceivingChannelAdapter receivingChannelAdapter = new TcpReceivingChannelAdapter();
        receivingChannelAdapter.setConnectionFactory( connectionFactory );
        receivingChannelAdapter.setOutputChannelName( "payloadFlow.input" );
        receivingChannelAdapter.setAutoStartup( false );
        receivingChannelAdapter.setRole( "leader" );

        return receivingChannelAdapter;
    }

    @Autowired
    TcpSendingMessageHandler sendingMessageHandler;

    @Autowired
    TcpReceivingChannelAdapter receivingChannelAdapter;

    @Autowired
    RegisterClient registerClient;

    @EventListener
    void handleTcpConnectionOpenEvent( final TcpConnectionOpenEvent event ) {
        log.info( "Opening TCP Connection {}", event );

        String instanceId = event.getConnectionId().substring( event.getConnectionId().lastIndexOf( ":" ) );
        clientManager.setInstanceId( instanceId );

        sendingMessageHandler.handleMessage( registerClient.sendRegister() );

    }

    @EventListener
    void handleOnGrantedEvent( final OnGrantedEvent event ) {
        log.info( "Leadership Granted {}", event );

    }

    @EventListener
    void handleOnRevokedEvent( final OnRevokedEvent event ) {
        log.info( "Leadership Revoked {}", event );

    }

}
