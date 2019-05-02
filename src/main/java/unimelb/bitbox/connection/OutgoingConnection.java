package unimelb.bitbox.connection;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.eventprocess.EventProcess;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class OutgoingConnection extends Connection{
    ArrayList<HostPort> queue;

    public OutgoingConnection(FileSystemManager fileSystemManager, ConnectionObserver observer, HostPort remoteHostPort) {
        this.queue = new ArrayList<>();

        this.fileSystemManager = fileSystemManager;
        this.connectionObserver = observer;
        this.remoteHostPort = null;
        this.socket = null;
        this.isIncoming = false;
        this.nRetries = 0;

        // Add the first host port to the queue
        queue.add(remoteHostPort);

        // Initialise threads
        this.listener = Executors.newSingleThreadExecutor();
        this.sender = Executors.newSingleThreadExecutor();
        this.background = Executors.newSingleThreadExecutor();

        listener.submit(new Handshake());
    }

    private class Handshake extends EventProcess {

        @Override
        public void run() {
            // Create a new socket for the connection
            while (!queue.isEmpty()) {
                log.info("attempting to start connection");
                remoteHostPort = queue.remove(0);

                // Create a socket for this peer
                try {
                    socket = new Socket(remoteHostPort.host, remoteHostPort.port);
                } catch (IOException e) {
                    log.info("unable to connect to the peer");
                    close();
                    return;
                }

                // Initialise the output and input
                initInputOutput();
                updateWriter(output);


                // Send a HANDSHAKE_REQUEST
                sendMessage(Messages.genHandshakeRequest(ServerMain.localHostPort));

                // Wait for response
                try {
                    Document doc = Document.parse(input.readLine());
                    String command = doc.getString("command");

                    // HANDSHAKE_RESPONSE, connection approved!
                    if (command.equals(Messages.HANDSHAKE_RESPONSE)) {
                        syncEvents();
                        listener.submit(new Listen());

                        log.info("successfully connected to peer " + remoteHostPort);
                        return;
                    }
                    // CONNECTION_REFUSED, add to queue and continue looking for peers
                    else if (command.equals(Messages.CONNECTION_REFUSED)) {
                        // Get the list of peers
                        Object o = doc.get("peers");
                        if (o instanceof ArrayList) {
                            ArrayList<Document> peerDocs = (ArrayList) o;

                            for (Document peerDoc : peerDocs) {
                                queue.add(new HostPort(peerDoc));
                            }

                            // Need to close the current socket and input and output
                            socket.close();
                            input.close();
                            output.close();
                        } else {
                            // Send INVALID_PROTOCOL
                            sendMessage(Messages.genInvalidProtocol("cannot access peers list"));
                            close();
                            return;
                        }
                    }
                    // Send an INVALID_PROTOCOL
                    else {
                        sendMessage(Messages.genInvalidProtocol("invalid command received"));
                        close();
                        return;
                    }
                } catch (IOException e) {
                    log.severe("error reading from peer");
                    close();
                    return;
                }
            }
        }
    }
}
