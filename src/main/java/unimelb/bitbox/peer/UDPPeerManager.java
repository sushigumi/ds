package unimelb.bitbox.peer;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.net.DatagramSocket;
import java.util.ArrayList;

public class UDPPeerManager {
    private final int MAXIMUM_PEERS = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));

    private ArrayList<UDPClient> rememberedPeers;
    private int nRememberedPeers;

    private static UDPPeerManager ourInstance = new UDPPeerManager();

    public static UDPPeerManager getInstance() {
        return ourInstance;
    }

    private UDPPeerManager() {
        rememberedPeers = new ArrayList<>();
        nRememberedPeers = 0;
    }

    /**
     * Add a peer to the connections. If there are too many connections, a CONNECTION_REFUSED message is sent back
     * and then the connection is removed from the list of remembered peers.
     * @param serverSocket
     * @param remoteHostPortString
     */
    public void addPeer(DatagramSocket serverSocket, String remoteHostPortString) {
        rememberedPeers.add(new UDPClient(serverSocket, new HostPort(remoteHostPortString)));
        nRememberedPeers++;
    }

    /**
     * Disconnect a peer and remove it from the list of remembered peers.
     * Thus, no more messages can be sent to it.
     * @param remoteHostPort
     */
    public void disconnectPeer(HostPort remoteHostPort) {
        UDPClient toRemove = null;

        for (UDPClient peer : rememberedPeers) {
            if (peer.getRemoteHostPort().equals(remoteHostPort)) {
                toRemove = peer;
                break;
            }
        }

        rememberedPeers.remove(toRemove);
        nRememberedPeers--;
    }

    /**
     * Process a file system event and send it to all the peers
     * @param fileSystemEvent
     */
    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        for (UDPClient peer : rememberedPeers) {
            peer.processFileSystemEvent(fileSystemEvent);
        }
    }

    /**
     * Check if the incoming connection can be accepted or not
     * @return True if there is still available space to accept a connection
     */
    public boolean isAvailableConnections() {
        return nRememberedPeers <= MAXIMUM_PEERS;
    }
}
