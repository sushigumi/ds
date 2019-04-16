package unimelb.bitbox.connection;

import unimelb.bitbox.messages.MessageCommands;
import unimelb.bitbox.messages.MessageGenerator;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
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
public class Connection {
    private Socket socket; // Other peer's socket

    private HostPort localHostPort;
    private HostPort remoteHostPort;

    private boolean isAlive; // Whether a TCP connection is kept alive

    private DataOutputStream output;
    private DataInputStream input;

    private Thread listener;
    private ExecutorService sender;

    /**
     * Called when receiving a connection from another peer
     * Only when receiving a connection do we add to the counter of connections based on the
     * maximumIncomingConnections parameter in the properties file
     * @param socket
     * @param localHostPort
     */
    public Connection(Socket socket, HostPort localHostPort) {
        this.socket = socket;
        this.localHostPort = localHostPort;
        this.isAlive = true;

        createWriterAndReader();

        // Synchronous part of the protocol. Exchanging handshakes
        // TODO:If an I/O error is caught, then perhaps we could try again for n number of times
        // TODO retry mechanism?
        // Wait for a handshake request to come
        try {
            Document message = Document.parse(input.readUTF());
            String command = message.getString("command");

            // If it is a HANDSHAKE_REQUEST then send a HANDSHAKE_RESPONSE, else send an INVALID_PROTOCOL
            if (command.equals(MessageCommands.HANDSHAKE_REQUEST.getCommand())) {
                HostPort remoteHostPort = new HostPort((Document)message.get("hostPort"));
                // If the maximum number of incoming connections has been reached, reject the connection
                // else accept the connection
                if (ConnectionManager.getInstance().isAnyFreeConnection()) {
                    output.writeUTF(MessageGenerator.genHandshakeResponse(localHostPort));

                    // Increment the number of incoming connections in the Connection Manager
                    ConnectionManager.getInstance().connectedPeer(remoteHostPort, true);
                } else {
                    // Send a CONNECTION_REFUSED message here
                    output.writeUTF(MessageGenerator.genConnectionRefused(ConnectionManager.getInstance().getPeers()));

                    // Close the connection
                    socket.close();
                }

            } else {
                output.writeUTF(MessageGenerator.genInvalidProtocol("Invalid command. Expecting HANDSHAKE_REQUEST"));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        // Create the thread to listen to messages
        listener = new Thread(new Listener());
        listener.start();

        // Create the single thread executor to send messages based on a queue when it requires messages to be
        // sent
        sender = Executors.newCachedThreadPool();
    }

    /**
     * Called when making a connection to another peer
     * So this peer needs to send a handshake request to the other peer
     * @param socket
     * @param localHostPort
     * @param remoteHostPort HostPort of the peer to be connected
     */
    public Connection(Socket socket, HostPort localHostPort, HostPort remoteHostPort) {
        this.socket = socket;
        this.localHostPort = localHostPort;
        this.remoteHostPort = remoteHostPort;
        this.isAlive = true;

        createWriterAndReader();

        // Synchronous part of the protocol. Exchanging handshakes
        // If an I/O error is caught, then perhaps we could try again for n number of times
        try {
            while (true) {
                // Send handshake request and wait for a response
                output.writeUTF(MessageGenerator.genHandshakeRequest(localHostPort));
                output.flush();
                Document response = Document.parse(input.readUTF());

                // Check if HANDSHAKE_RESPONSE or CONNECTION_REFUSED
                String command = response.getString("command");
                if (command.equals(MessageCommands.HANDSHAKE_RESPONSE.getCommand())) {
                    ConnectionManager.getInstance().connectedPeer(remoteHostPort, false);
                    break;
                } else if (command.equals(MessageCommands.CONNECTION_REFUSED)) {
                    // If Connection is refused,
                    //TODO
                    // socket.close();
                    break;
                } else {
                    // TODO send an invalid protocol
                    MessageGenerator.genInvalidProtocol("Invalid command. Expecting HANDSHAKE_RESPONSE " +
                            "or CONNECTION_REFUSED");
                }

            }
        } catch (IOException e) {
            System.out.println("IOException occurred in sending or receiving handshake");
            e.printStackTrace();
        }

        // Create the thread to listen to messages
        listener = new Thread(new Listener());
        listener.start();

        // Create a single thread executor to send messages based on a queue when it requires messages to be sent
        sender = Executors.newSingleThreadExecutor();

    }

    private void createWriterAndReader() {
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

    /**
     * A runnable to only listen and receive messages from other peers
     */
    private class Listener implements Runnable {
        @Override
        public void run() {
            // If cannot read what is being sent, then just ignore and wait for the other peer to send
            // the message again
            try {
                while (true) {
                    String in = input.readUTF();

                    Document doc = Document.parse(in);

                    // Create a new thread
                    // ?? switch?
                    switch (command) {
                        // FILE_CREATE REQUEST
                    }

                    System.out.println(in);
                }
            } catch (IOException e) {

            }
        }
    }
}
