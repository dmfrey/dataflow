package com.vmware.dmfrey.dataflow.tcpclient;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.messaging.MessageHeaders;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
@IntegrationComponentScan
public class TcpIntegrationFlowsConfig {

    @Bean
    public IntegrationFlow heartbeatFlow( final TcpSendingMessageHandler sendingMessageHandler, final HeartbeatClient heartbeatClient ) {

        return IntegrationFlows.from( heartbeatClient::sendHeartbeat, e -> e.poller( Pollers.fixedDelay( 5, TimeUnit.SECONDS, 5 ) ) )
                .handle( sendingMessageHandler )
                .get();
    }

    @Bean
    public IntegrationFlow payloadFlow( final StreamBridge streamBridge ) {

        return f -> f
                .<byte[], String>transform( String::new )
                .filter( payload -> ( (String) payload ).startsWith( "payload:" )  )
                .handle( (p, h) -> {
                    String[] message = ( (String) p ).split( ":" );

                    var id = (UUID) h.get( MessageHeaders.ID );
                    var connectionId = (String) h.get( IpHeaders.CONNECTION_ID );
                    var ipAddress = (String) h.get( IpHeaders.IP_ADDRESS );
                    var hostname = (String) h.get( IpHeaders.HOSTNAME );
                    var timestamp = (long) h.get( MessageHeaders.TIMESTAMP );

                    return new InternalMessage( message[ 5 ] + ":" + message[ 6 ] + ":" + message[ 7 ], id.toString(), connectionId, ipAddress, hostname, timestamp );
                })
                .log()
                .handle( (p, h) -> {
                    streamBridge.send( "adapter-out-0", p );

                    return null;
                });
    }

}

class InternalMessage {

    @JsonProperty
    final String payload;

    @JsonProperty
    final String id;

    @JsonProperty
    final String connectionId;

    @JsonProperty
    final String ipAddress;

    @JsonProperty
    final String hostname;

    @JsonProperty
    final long timestamp;

    InternalMessage( final String payload, final String id, final String connectionId, final String ipAddress, final String hostname, final long timestamp ) {

        this.payload = payload;
        this.id = id;
        this.connectionId = connectionId;
        this.ipAddress = ipAddress;
        this.hostname = hostname;
        this.timestamp = timestamp;

    }

    @Override
    public String toString() {
        return "InternalMessage{" +
                "payload='" + payload + '\'' +
                ", id='" + id + '\'' +
                ", connectionId='" + connectionId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", hostname='" + hostname + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

}