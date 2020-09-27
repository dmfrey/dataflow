package com.vmware.dmfrey.dataflow.tcpclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatClient {

    private static final Logger log = LoggerFactory.getLogger( HeartbeatClient.class );

    private final ClientManager clientManager;

    public HeartbeatClient( final ClientManager clientManager ) {

        this.clientManager = clientManager;

    }

    GenericMessage<String> sendHeartbeat() {

        return new GenericMessage<>( String.format( "heartbeat:%s", clientManager.getClientId() ) );
    }

}
