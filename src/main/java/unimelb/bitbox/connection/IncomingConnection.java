package unimelb.bitbox.connection;

import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.eventprocess.BaseRunnable;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;

public class IncomingConnection extends Connection {
    public IncomingConnection(FileSystemManager fileSystemManager, Socket socket, HostPort localHostPort) {
        super(fileSystemManager, socket, localHostPort);

        // Submit a Handshake runnable to the listener
        listener.submit(new Handshake(output));
    }

    private class Handshake extends BaseRunnable {
        Handshake(BufferedWriter output) {
            super(output);
        }

        @Override
        public void run() {
            // Synchronous part of the protocol. Exchanging handshakes
            // TODO:If an I/O error is caught, then perhaps we could try again for n number of times
            // TODO retry mechanism?
            // Wait for a handshake request to come
            try {
                Document message = Document.parse(input.readLine());
                //System.out.println(message.toJson());
                String command = message.getString("command");

                // If it is a HANDSHAKE_REQUEST then send a HANDSHAKE_RESPONSE, else send an INVALID_PROTOCOL
                if (command.equals(Messages.HANDSHAKE_REQUEST)) {
                    HostPort remoteHostPort = new HostPort((Document)message.get("hostPort"));
                    // If the maximum number of incoming connections has been reached, reject the connection
                    // else accept the connection
                    if (ConnectionManager.getInstance().isAnyFreeConnection()) {
                        sendMessage(Messages.genHandshakeResponse(localHostPort));
                        listener.submit(new Listener());

                        // Increment the number of incoming connections in the Connection Manager
                        updateRemoteHostPort(remoteHostPort);
                        ConnectionManager.getInstance().connectedPeer(remoteHostPort, true);

                        // TODO Call generate sync events here and sent appropriate messages
                        initSyncPeers();
                    } else {
                        // Send a CONNECTION_REFUSED message here
                        sendMessage(Messages.genConnectionRefused(ConnectionManager.getInstance().getPeers()));
                        System.out.println("connection refused");

                        // Sleep this thread to wait for the message to be sent fully then close the socket
//                        try {
//                            Thread.sleep(1000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }

                        System.out.println("Connection closed");
                        output.close();
                        input.close();
                        observer.closeConnection(remoteHostPort);
                    }

                } else {
                    sendMessage(Messages.genInvalidProtocol("Invalid command. Expecting HANDSHAKE_REQUEST"));
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
