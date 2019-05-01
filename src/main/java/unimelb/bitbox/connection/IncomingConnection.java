package unimelb.bitbox.connection;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.eventprocess.BaseRunnable;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;

public class IncomingConnection extends Connection {
    IncomingConnection(FileSystemManager fileSystemManager, Socket socket, ConnectionObserver connectionObserver) {
        super(fileSystemManager, socket, connectionObserver);

        // Create the writer and reader
        createWriterAndReader();

        // Start the handshake
        listener.submit(new Handshake());
    }

    private class Handshake extends BaseRunnable {
        Handshake() {
            super(output);
        }

        @Override
        public void run() {
            // Listen for a message from the peer
            try {
                String msg = input.readLine();
                Document doc = Document.parse(msg);

                String command = doc.getString("command");

                // HANDSHAKE_REQUEST
                if (command.equals(Messages.HANDSHAKE_REQUEST)) {
                    // If still have space to accept the connection, send a HANDSHAKE_RESPONSE
                    remoteHostPort = new HostPort((Document)doc.get("hostPort"));
                    connectionObserver.updateRetries(remoteHostPort); // Update the number of retries

                    // Send HANDSHAKE_RESPONSE
                    if (ConnectionManager.getInstance().isAnyFreeConnection()) {
                        sendMessage(Messages.genHandshakeResponse(ServerMain.localHostPort));
                        listener.submit(new Listener());

                        // Sync peers
                        initSyncPeers();

                        log.info("successfully connected to " + remoteHostPort);
                    }
                    // Send CONNECTION_REFUSED
                    else {
                        log.info("maximum number of incoming connections reached, closing connection");
                        sendMessage(Messages.genConnectionRefused(ConnectionManager.getInstance().getPeersHostPort(remoteHostPort)));
                        close();
                    }
                }
                // Received invalid message, so send INVALID_PROTOCOL
                else {
                    log.info("invalid message received");
                    sendMessage(Messages.genInvalidProtocol("expecting HANDSHAKE_REQUEST"));
                    close();
                }
            }
            // Found an error, close the connection and report to user
            catch (IOException e) {
                log.severe("error happened when the peer is trying to listen for a HANDSHAKE_REQUEST");
                close();
            }
        }
    }
}
