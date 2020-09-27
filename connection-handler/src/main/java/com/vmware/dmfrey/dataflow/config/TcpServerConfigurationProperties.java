package com.vmware.dmfrey.dataflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties( prefix = "tcp-server" )
public class TcpServerConfigurationProperties {

    private String url;
    private int port;
    private int poolSize;

    public TcpServerConfigurationProperties() { }

    public TcpServerConfigurationProperties( final String url, final int port, int poolSize) {

        this.url = url;
        this.port = port;
        this.poolSize = poolSize;

    }

    public void setUrl( final String url ) {

        this.url = url;

    }

    public String getUrl() {

        return url;
    }

    public void setPort( final int port ) {

        this.port = port;

    }

    public int getPort() {

        return port;
    }

    public void setPoolSize(int poolSize) {

        this.poolSize = poolSize;

    }

    public int getPoolSize() {

        return poolSize;
    }

}
