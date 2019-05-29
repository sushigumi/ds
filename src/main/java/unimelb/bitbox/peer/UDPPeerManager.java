package unimelb.bitbox.peer;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.logging.Logger;

public class UDPPeerManager {
    private static Logger log = Logger.getLogger(UDPPeerManager.class.getName());

    private final int MAXIMUM_PEERS = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));

    private ArrayList<UDPPeer> rememberedPeers;
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
        rememberedPeers.add(new UDPPeer(fileSystemManager, serverSocket, new HostPort(remoteHostPortString), false));
        nRememberedPeers++;

        log.info("adding peer " + remoteHostPortString);
    }

    /**
     * Add a peer to the connections, Called for an incoming connection
     * @param serverSocket
     * @param remoteHostPort
     */
    public void addPeer(DatagramSocket serverSocket, HostPort remoteHostPort, FileSystemManager fileSystemManager) {
        rememberedPeers.add(new UDPPeer(fileSystemManager, serverSocket, remoteHostPort, true));
        nRememberedPeers++;
        log.info("adding peer " + remoteHostPort.toString());
    }

    /**
     * Disconnect a peer and remove it from the list of remembered peers.
     * Thus, no more messages can be sent to it.
     * @param remoteHostPort
     */
    public void disconnectPeer(HostPort remoteHostPort) {
        UDPPeer toRemove = null;

        for (UDPPeer peer : rememberedPeers) {
            if (peer.getRemoteHostPort().equals(remoteHostPort)) {
                toRemove = peer;
                break;
            }
        }

        rememberedPeers.remove(toRemove);
        nRememberedPeers--;

        // Shutdown the threads
        if (toRemove != null) toRemove.shutdown();

        log.info("disconnected peer " + remoteHostPort.toString() + " due to inactivity");

        System.out.println(rememberedPeers.size());
    }

    /**
     * Get the host ports of the remembered peers
     * @return
     */
    public ArrayList<HostPort> getConnectedPeers() {
        ArrayList<HostPort> hostPorts = new ArrayList<>();
        for (UDPPeer peer : rememberedPeers) {
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
        for (UDPPeer peer : rememberedPeers) {
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
        UDPPeer toRetry = null;
        // Search for the peer
        for (UDPPeer peer : rememberedPeers) {
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
    public UDPPeer.STATE getStateOfPeer(HostPort remoteHostPort) {
        for (UDPPeer peer : rememberedPeers) {
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
    public void setStateOfPeer(HostPort remoteHostPort, UDPPeer.STATE state) {
        for (UDPPeer peer : rememberedPeers) {
            if (peer.getRemoteHostPort().equals(remoteHostPort)) {
                peer.setState(state);

                // If just became OK then we need to sync events
                if (state == UDPPeer.STATE.OK) {
                    peer.syncEvents();
                }
                return;
            }
        }
    }

    /**
     * Get a UDPPeer based on its remote host port
     * @param remoteHostPort
     * @return
     */
    public UDPPeer getPeer(HostPort remoteHostPort) {
        for (UDPPeer peer : rememberedPeers) {
            if (peer.getRemoteHostPort().equals(remoteHostPort)) {
                return peer;
            }
        }

        return null;
    }
}
