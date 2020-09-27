package com.vmware.dmfrey.dataflow.tcpclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

@Component
public class RegisterClient {

    private static final Logger log = LoggerFactory.getLogger( RegisterClient.class );

    private final ClientManager clientManager;

    public RegisterClient(final ClientManager clientManager ) {

        this.clientManager = clientManager;

    }

    GenericMessage<String> sendRegister() {

        return new GenericMessage<>( String.format( "register:%s", clientManager.getClientId() ) );
    }

}
