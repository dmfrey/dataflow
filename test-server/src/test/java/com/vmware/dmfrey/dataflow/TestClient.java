package com.vmware.dmfrey.dataflow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection( String ip, int port ) throws IOException {

        this.clientSocket = new Socket( ip, port );
        this.out = new PrintWriter( this.clientSocket.getOutputStream(), true );
        this.in = new BufferedReader( new InputStreamReader( this.clientSocket.getInputStream() ) );

//        String received;
//        while( ( received = in.readLine() ) != null ) {
//
//            System.out.println( received );
//
//        }

    }

    public String sendMessage( String msg ) throws IOException {

        if( null == out ) {

            throw new IOException( "connection has been closed" );
        }

        this.out.println( msg );

        String resp = this.in.readLine();
        if( resp.startsWith( "closing client connection" ) ) {
            stopConnection();
        }

        return resp;
    }

    public void stopConnection() throws IOException {

        if( null != this.in ) this.in.close();
        if( null != this.out ) this.out.close();
        if( null != this.clientSocket ) this.clientSocket.close();

    }

}
