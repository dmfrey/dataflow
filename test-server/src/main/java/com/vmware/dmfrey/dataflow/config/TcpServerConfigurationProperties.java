package com.vmware.dmfrey.dataflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties( prefix = "tcp-server" )
public class TcpServerConfigurationProperties {

    private int port;

    TcpServerConfigurationProperties() { }

    TcpServerConfigurationProperties( final int port ) {

        this.port = port;

    }

    public int getPort() {

        return port;
    }

    public void setPort(int port) {

        this.port = port;

    }

}
