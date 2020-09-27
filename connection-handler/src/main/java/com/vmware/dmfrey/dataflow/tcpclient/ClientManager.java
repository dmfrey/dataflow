package com.vmware.dmfrey.dataflow.tcpclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientManager {

    @Value( "${client.id}" )
    private String clientId;

    private String instanceId;

    String getClientId() {

        return clientId;
    }

    String getInstanceId() {

        return instanceId;
    }

    void setInstanceId( final String instanceId ) {

        this.instanceId = instanceId;

    }

}
