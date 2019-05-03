package unimelb.bitbox.connection;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.eventprocess.EventProcess;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executors;

public class IncomingConnection extends Connection {
    public IncomingConnection(FileSystemManager fileSystemManager, ConnectionObserver observer, Socket socket) {
        this.fileSystemManager = fileSystemManager;
        this.connectionObserver = observer;
        this.remoteHostPort = null;
        this.socket = socket;
        this.isIncoming = true;
        this.nRetries = 0;

        // Initialise threads
        this.listener = Executors.newSingleThreadExecutor();
        this.sender = Executors.newSingleThreadExecutor();
        this.background = Executors.newSingleThreadExecutor();

        listener.submit(new Handshake());
    }

    private class Handshake extends EventProcess {

        @Override
        public void run() {
            // Create the reader and writer and update writer
            initInputOutput();
            updateWriter(output);

            // Get message received
            try {
                Document doc = Document.parse(input.readLine());
                String command = doc.getString("command");

                // HANDSHAKE_REQUEST received
                if (command.equals(Messages.HANDSHAKE_REQUEST)) {
                    // If there are still available connections then send HANDSHAKE_RESPONSE
                    if (ConnectionManager.getInstance().isAvailableConnections()) {
                        remoteHostPort = new HostPort((Document)doc.get("hostPort"));
                        sendMessage(Messages.genHandshakeResponse(ServerMain.localHostPort));

                        syncEvents();

                        // Start listening for other messages from the peer
                        listener.submit(new Listen());

                        log.info("successfully connected to peer " + remoteHostPort);
                    }
                    // Otherwise CONNECTION_REFUSED
                    else {
                        sendMessage(Messages.genConnectionRefused(ConnectionManager.getInstance().getPeersHostPorts()));

                        // Close the connection
                        close();
                    }
                }
                // Other message received, respond with INVALID_PROTOCOL and close connection
                else {
                    sendMessage(Messages.genInvalidProtocol("expecting HANDSHAKE_REQUEST"));
                    close();
                }

            } catch (IOException e) {
                log.severe("error receiving message from peer");
            }

        }
    }
}
