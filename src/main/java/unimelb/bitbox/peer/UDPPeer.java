package unimelb.bitbox.peer;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.eventprocess.*;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//TODO add retry mechanism
public class UDPPeer {
    public enum STATE {
        HANDSHAKE, // In the middle of a handshake process
        OK; // After handshake successful
    }

    private ExecutorService sender;

    private STATE state;
    private boolean isIncoming;

    private FileSystemManager fileSystemManager;

    private DatagramSocket serverSocket;
    private HostPort remoteHostPort;

    private ArrayList<HostPort> queue;

    public UDPPeer(FileSystemManager fileSystemManager, DatagramSocket serverSocket, HostPort remoteHostPort, boolean isIncoming) {
        this.serverSocket = serverSocket;
        this.remoteHostPort = null;
        this.fileSystemManager = fileSystemManager;
        sender = Executors.newSingleThreadExecutor();
        this.isIncoming = isIncoming;

        // Add the host port to the queue
        queue = new ArrayList<>();
        queue.add(remoteHostPort);

        // Set the state
        this.state = STATE.HANDSHAKE;

        // Start the handshake process
        if (isIncoming) {
            // Set the host port since it is an incoming connection and we will not receive a CONNECTION_REFUSED
            this.remoteHostPort = remoteHostPort;
            onNewIncoming();
        } else {
            onNewOutgoing();
        }
    }

    /**
     * Start a handshake, sends a HANDSHAKE_REQUEST to the specified peer to request for a connection. If the connection
     * is refused, then tries to connect with all the others returned from the CONNECTION_REFUSED message.
     */
    private void onNewOutgoing() {
        this.remoteHostPort = queue.remove(0);

        // Send a HANDSHAKE_REQUEST to the peer
        sender.submit(new HandshakeReq(serverSocket, remoteHostPort));
    }

    /**
     * Calls generateSyncEvents() from the file system manager and sends it to the peers. Usually called at the
     * start of a peer.
     */
    void syncEvents() {
        ArrayList<FileSystemManager.FileSystemEvent> events = fileSystemManager.generateSyncEvents();

        for (FileSystemManager.FileSystemEvent event : events) {
            processFileSystemEvent(event);
        }
    }

    /**
     * Send a HANDSHAKE_RESPONSE or CONNECTION_REFUSED depending if there are available connections left for the peer
     */
    private void onNewIncoming() {
        // Send HANDSHAKE_RESPONSE
        if (UDPPeerManager.getInstance().isAvailableConnections()) {
            sender.submit(new HandshakeRes(serverSocket, remoteHostPort));
            state = STATE.OK;

            // Generate the sync events
            syncEvents();
        }
        // Send CONNECTION_REFUSED
        else {
            sender.submit(new EventProcess() {
                @Override
                public void run() {
                    sendMessage(Messages.genConnectionRefused(UDPPeerManager.getInstance().getConnectedPeers()));
                }
            });

            // Disconnect the peer
            UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
        }
    }

    /**
     * Called after a CONNECTION_REFUSED message is received in the ServerMain.
     */
    public void tryOtherPeer(ArrayList<HostPort> otherPeers) {
        // Add all the other peers to the end of the queue so that it simulates a bfs
        queue.addAll(otherPeers);

        // Start a new connection
        onNewIncoming();
    }

    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        switch(fileSystemEvent.event) {
            case FILE_CREATE:
                sender.submit(new FileCreateRequest(serverSocket, remoteHostPort, fileSystemEvent));
                break;

            case FILE_DELETE:
                sender.submit(new FileDeleteRequest(serverSocket, remoteHostPort, fileSystemEvent));
                break;

            case FILE_MODIFY:
                sender.submit(new FileModifyRequest(serverSocket, remoteHostPort, fileSystemEvent));
                break;

            case DIRECTORY_CREATE:
                sender.submit(new DirectoryCreateRequest(serverSocket, remoteHostPort, fileSystemEvent.pathName));
                break;

            case DIRECTORY_DELETE:
                sender.submit(new DirectoryDeleteRequest(serverSocket, remoteHostPort, fileSystemEvent.pathName));
                break;
        }
    }

    /**
     * Get the host port of the client
     * @return
     */
    public HostPort getRemoteHostPort() {
        return remoteHostPort;
    }

    /**
     * Get the current state of the UDP client
     * @return
     */
    public STATE getState() {
        return state;
    }

    /**
     * Set the state of the UDP client
     * @param state
     */
    public void setState(STATE state) {
        this.state = state;
    }

    /**
     * Send a handshake request to the peer
     */
    private class HandshakeReq extends EventProcess {
        public HandshakeReq(DatagramSocket socket, HostPort hostPort) {
            super(socket, hostPort);
        }

        @Override
        public void run() {
            // Send handshake request
            sendMessage(Messages.genHandshakeRequest(ServerMain.getLocalHostPort()));
        }
    }

    private class HandshakeRes extends EventProcess {
        public HandshakeRes(DatagramSocket socket, HostPort hostPort) {
            super(socket, hostPort);
        }

        @Override
        public void run() {
            // Send handshake response
            sendMessage(Messages.genHandshakeResponse(ServerMain.getLocalHostPort()));
        }
    }

}