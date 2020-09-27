package com.vmware.dmfrey.dataflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestServerTests {

    static String serverIp = "127.0.0.1";
    static int serverPort = 6666;

    TestClient client1;
    TestClient client2;
    TestClient client3;
    TestClient client4;

    @BeforeEach
    void setup() throws IOException {

        this.client1 = new TestClient();
        this.client1.startConnection( serverIp, serverPort );

        this.client2 = new TestClient();
        this.client2.startConnection( serverIp, serverPort );

        this.client3 = new TestClient();
        this.client3.startConnection( serverIp, serverPort );

        this.client4 = new TestClient();
        this.client4.startConnection( serverIp, serverPort );

    }

    @Test
    void whenClient1Connects_verifyClientRegistrationReceived() throws IOException {

        String clientId = "client-1";
        String registeredMessage = this.client1.sendMessage( String.format( "register:%s", clientId ) );
        String heartbeatMessage1 = this.client1.sendMessage( String.format( "heartbeat:%s", clientId ) );
        String heartbeatMessage2 = this.client1.sendMessage( String.format( "heartbeat:%s", clientId ) );
        String heartbeatMessage3 = this.client1.sendMessage( String.format( "heartbeat:%s", clientId ) );
        String disconnectMessage = this.client1.sendMessage( String.format( "close:%s", clientId ) );

        assertEquals( String.format( "registered client connection : %s", clientId ), registeredMessage );
        assertEquals( String.format( "client connection verified : %s", clientId ), heartbeatMessage1 );
        assertEquals( String.format( "client connection verified : %s", clientId ), heartbeatMessage2 );
        assertEquals( String.format( "client connection verified : %s", clientId ), heartbeatMessage3 );
        assertEquals( String.format( "closing client connection : %s", clientId ), disconnectMessage );

    }

    @Test
    void whenClient2Connects_verifyClientRegistrationReceived() throws IOException {

        String clientId = "client-2";
        String registeredMessage = this.client2.sendMessage( String.format( "register:%s", clientId ) );
        String heartbeatMessage = this.client2.sendMessage( String.format( "heartbeat:%s", clientId ) );
        String disconnectMessage = this.client2.sendMessage( String.format( "close:%s", clientId ) );

        assertEquals( String.format( "registered client connection : %s", clientId ), registeredMessage );
        assertEquals( String.format( "client connection verified : %s", clientId ), heartbeatMessage );
        assertEquals( String.format( "closing client connection : %s", clientId ), disconnectMessage );

    }

    @Test
    void whenClient3Connects_verifyClientRegistrationReceived_thenSendMessageAfterDisconnect() throws IOException {

        String clientId = "client-3";
        String registeredMessage = this.client3.sendMessage( String.format( "register:%s", clientId ) );
        String heartbeatMessage = this.client3.sendMessage( String.format( "heartbeat:%s", clientId ) );
        String disconnectMessage = this.client3.sendMessage( String.format( "close:%s", clientId ) );

        assertEquals( String.format( "registered client connection : %s", clientId ), registeredMessage );
        assertEquals( String.format( "client connection verified : %s", clientId ), heartbeatMessage );
        assertEquals( String.format( "closing client connection : %s", clientId ), disconnectMessage );

        assertThrows( IOException.class, () ->
                this.client3.sendMessage( String.format( "heartbeat:%s", clientId ) )
        );

    }

    @Test
    void whenClient4Connects_verifyClientRegistrationReceived_thenWaitForHeartbeatFailure() throws IOException {

        String clientId = "client-4";
        String registeredMessage = this.client4.sendMessage( String.format( "register:%s", clientId ) );

        assertEquals( String.format( "registered client connection : %s", clientId ), registeredMessage );

        try {

            Thread.sleep( 13000 );
            this.client4.sendMessage( String.format( "heartbeat:%s", clientId ) );

        } catch( InterruptedException e ) {

            e.printStackTrace();

        }

    }

}
