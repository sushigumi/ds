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
     * Add a peer to the connections. Called for an outgoing connection
     * @param serverSocket
     * @param remoteHostPortString
     */
    public void addPeer(DatagramSocket serverSocket, String remoteHostPortString, FileSystemManager fileSystemManager) {
        rememberedPeers.add(new UDPClient(fileSystemManager, serverSocket, new HostPort(remoteHostPortString), false));
        nRememberedPeers++;
    }

    /**
     * Add a peer to the connections, Called for an incoming connection
     * @param serverSocket
     * @param remoteHostPort
     */
    public void addPeer(DatagramSocket serverSocket, HostPort remoteHostPort, FileSystemManager fileSystemManager) {
        rememberedPeers.add(new UDPClient(fileSystemManager, serverSocket, remoteHostPort, true));
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
     * Get the host ports of the remembered peers
     * @return
     */
    public ArrayList<HostPort> getConnectedPeers() {
        ArrayList<HostPort> hostPorts = new ArrayList<>();
        for (UDPClient peer : rememberedPeers) {
            if (peer.getRemoteHostPort() != null) {
                hostPorts.add(peer.getRemoteHostPort());
            }
        }

        return hostPorts;
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

    /**
     * Called when the server receives a CONNECTION_REFUSED message and the appropriate client needs to search for
     * another peer to connect to
     * @param remoteHostPort
     * @param otherPeers
     */
    public void onConnectionRefused(HostPort remoteHostPort, ArrayList<HostPort> otherPeers) {
        UDPClient toRetry = null;
        // Search for the peer
        for (UDPClient peer : rememberedPeers) {
            if (peer.getRemoteHostPort().equals(remoteHostPort)) {
                toRetry = peer;
                break;
            }
        }

        // If the peer exists, then retry
        if (toRetry != null) {
            toRetry.tryOtherPeer(otherPeers);
        }
    }

    /**
     * Get the state of the appropriate Peer
     * @param remoteHostPort
     * @return
     */
    public UDPClient.STATE getStateOfPeer(HostPort remoteHostPort) {
        for (UDPClient peer : rememberedPeers) {
            if (peer.getRemoteHostPort().equals(remoteHostPort)) {
                return peer.getState();
            }
        }

        return null;
    }

    /**
     * Set the state of the selected peer
     * @param remoteHostPort
     */
    public void setStateOfPeer(HostPort remoteHostPort, UDPClient.STATE state) {
        for (UDPClient peer : rememberedPeers) {
            if (peer.getRemoteHostPort().equals(remoteHostPort)) {
                peer.setState(state);

                // If just became OK then we need to sync events
                if (state == UDPClient.STATE.OK) {
                    peer.syncEvents();
                }
                return;
            }
        }
    }
}
