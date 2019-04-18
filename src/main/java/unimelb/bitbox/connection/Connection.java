package unimelb.bitbox.connection;

import unimelb.bitbox.messages.Commands;
import unimelb.bitbox.messages.MessageGenerator;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents a TCP connection between two peers.
 * Before asynchronous moving on to the asynchronous part of the protocol,
 * connections must either send a HANDSHAKE_REQUEST message and wait for a HANDSHAKE_RESPONSE message
 * or receive a HANDSHAKE_REQUEST and respond with a HANDSHAKE_RESPONSE message
 * If a HANDSHAKE_REQUEST is received but the maximum number of connections has been achieved, then
 * a CONNECTION_REFUSED message must be sent back to the peer and the connection is closed.
 * Similarly, if a CONNECTION_REFUSED message is received, the connection must be closed.
 */
public abstract class Connection {
    Socket socket; // Other peer's socket

    HostPort localHostPort;

    DataOutputStream output;
    DataInputStream input;

    ExecutorService listener;
    ExecutorService sender;
    ExecutorService background;

    /**
     * Called when receiving a connection from another peer
     * Only when receiving a connection do we add to the counter of connections based on the
     * maximumIncomingConnections parameter in the properties file
     * @param socket
     * @param localHostPort
     */
    Connection(Socket socket, HostPort localHostPort) {
        this.socket = socket;
        this.localHostPort = localHostPort;

        createWriterAndReader();

        this.listener = Executors.newSingleThreadExecutor();
        this.sender = Executors.newSingleThreadExecutor();

        // Create the single thread executor to send messages based on a queue when it requires messages to be
        // sent
        this.background = Executors.newSingleThreadExecutor();
    }

    /**
     * Called when making a connection to another peer
     * So this peer needs to send a handshake request to the other peer
     * @param localHostPort
     */
    Connection(HostPort localHostPort) {
        this.localHostPort = localHostPort;

        this.listener = Executors.newSingleThreadExecutor();
        this.sender = Executors.newSingleThreadExecutor();
        this.background = Executors.newSingleThreadExecutor();
    }

    void createWriterAndReader() {
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    class Listener implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    String in = input.readUTF();

                    Document doc = Document.parse(in);

                    System.out.println(in);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
