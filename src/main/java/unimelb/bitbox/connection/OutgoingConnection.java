package unimelb.bitbox.connection;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.eventprocess.BaseRunnable;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class OutgoingConnection extends Connection {
    OutgoingConnection(FileSystemManager fileSystemManager, ConnectionObserver connectionObserver, HostPort remoteHostPort) {
        super(fileSystemManager, connectionObserver);

        // Start the handshake
        listener.submit(new Handshake(remoteHostPort));
    }

    private class Handshake extends BaseRunnable {
        private HostPort firstHostPort;
        Handshake(HostPort firstHostPort) {
            super(null);

            this.firstHostPort = firstHostPort;

        }

        @Override
        public void run() {
            // Start a queue to add peers to use bfs to connect to peers if received a CONNECTION_REFUSED
            // message
            LinkedList<HostPort> queue = new LinkedList<>();
            queue.add(firstHostPort);

            // While the queue is not empty, get the peers one by one and connect to them until
            // a successful connection is established
            while (!queue.isEmpty()) {
                remoteHostPort = queue.removeFirst();

                // Create a new socket for the current peer if failed, just exit
                try {
                    socket = new Socket(remoteHostPort.host, remoteHostPort.port);
                } catch (Exception e) {
                    log.info("error creating socket. could not connect to peer " + remoteHostPort.toString());
                    close();
                    return;
                }

                // Create reader and writer for the connection and update the output
                createWriterAndReader();
                updateOutput(output);

                // Initiate a HANDSHAKE_REQUEST to the peer
                sendMessage(Messages.genHandshakeRequest(ServerMain.localHostPort));

                // Now wait for a response
                try {
                    String res = input.readLine();

                    //System.out.println("Received: " + res);

                    Document resDoc = Document.parse(res);
                    String command = resDoc.getString("command");

                    // HANDSHAKE_RESPONSE
                    if (command.equals(Messages.HANDSHAKE_RESPONSE)) {
                        // Connection successful! Submit a listener to the thread, sync peers and return
                        listener.submit(new Listener());
                        initSyncPeers();
                        log.info("successfully connected to " + remoteHostPort);
                        return;
                    }
                    // CONNECTION_REFUSED
                    else if (command.equals(Messages.CONNECTION_REFUSED)) {
                        // Connection refused :( Time to look for a new connection
                        // Get the peers
                        // Check if the peers received is a correct JSON list, otherwise send an INVALID_PROTOCOL
                        Object o = resDoc.get("peers");
                        if (o instanceof ArrayList) {
                            // Close everything
                            try {
                                socket.close();
                                socket = null;
                                input.close();
                                input = null;
                                output.close();
                                output = null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // Append to the queue
                            ArrayList<Document> peersDocs = (ArrayList)o;
                            for (Document peerDoc : peersDocs) {
                                queue.add(new HostPort(peerDoc));
                            }

                        } else {
                            // Send invalid protocol and try to connect to the next peer
                            sendMessage(Messages.genInvalidProtocol("peers should be a list"));
                            close();
                        }
                    }
                    // Anything else so send an INVALID_PROTOCOL
                    else {
                        sendMessage(Messages.genInvalidProtocol("expecting HANDSHAKE_RESPONSE or CONNECTION_REFUSED"));
                        close();
                        return;
                    }
                } catch (IOException e) {
                    log.severe("error happened when waiting for peer to respond to HANDSHAKE_REQUEST");
                    // Close the connection
                    close();
                }
            }

            // Reached here if unable to connect to all the peers
            log.info("couldn't connect to any peer, all peers may be offline or in an error state");
            close();
        }
    }
}
