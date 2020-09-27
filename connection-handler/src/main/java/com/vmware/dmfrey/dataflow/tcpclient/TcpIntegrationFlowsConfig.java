package com.vmware.dmfrey.dataflow.tcpclient;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;

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
                .transform( payload -> ( (String) payload ).substring( ( (String) payload ).lastIndexOf( ":" ) + 1 ) )
                .log()
                .handle( (p, h) -> {
                    streamBridge.send( "adapter-out-0", p );

                    return null;
                });
    }

}
