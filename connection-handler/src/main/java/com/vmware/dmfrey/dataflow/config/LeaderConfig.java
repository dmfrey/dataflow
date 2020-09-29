package com.vmware.dmfrey.dataflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.leader.LockRegistryLeaderInitiator;
import org.springframework.integration.support.locks.LockRegistry;

@Configuration
public class LeaderConfig {

    @Bean
    LockRegistry lockRegistry( final RedisConnectionFactory redisConnectionFactory ) {

        return new RedisLockRegistry( redisConnectionFactory, "connection-handler" );
    }

    @Bean
    LockRegistryLeaderInitiator lockRegistryLeaderInitiator( final LockRegistry lockRegistry ) {

        return new LockRegistryLeaderInitiator( lockRegistry );
    }

}
