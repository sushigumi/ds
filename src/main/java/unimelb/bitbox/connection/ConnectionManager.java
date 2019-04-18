package unimelb.bitbox.connection;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Handles connections and is a singleton
 */
public class ConnectionManager {
    public final int MAXIMUM_CONNECTIONS;
    private int nConnections;  // number of incoming connections currently active

    private ArrayList<HostPort> peers;
    private ArrayList<Connection> peerConnections;

    private static ConnectionManager instance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return instance;
    }

    private ConnectionManager() {
        MAXIMUM_CONNECTIONS = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
        peers = new ArrayList<>();
        nConnections = 0;
        peerConnections = new ArrayList<>();
    }

    /**
     * For each peer in peers calls the method addPeer to set up a connection to each of the
     * peers
     * @param peers
     */
    public void addPeers(String[] peers, HostPort localHostPort) {
        for (String peer : peers) {
            HostPort remoteHostPort = new HostPort(peer);

           addPeer(localHostPort, remoteHostPort);

        }
    }

    /**
     * Add a peer when making a connection from this peer to another.
     * Local peer is the client
     * @param localHostPort
     * @param remoteHostPort
     */
    public void addPeer(HostPort localHostPort, HostPort remoteHostPort) {
        Connection connection = new OutgoingConnection(localHostPort, remoteHostPort);
        peerConnections.add(connection);
    }

    /**
     * Add a peer when accepting a connection from another peer
     * Local peer is the server
     * @param socket
     * @param localHostPort
     */
    public void addPeer(Socket socket, HostPort localHostPort) {
        Connection connection = new IncomingConnection(socket, localHostPort);
        peerConnections.add(connection);
    }

    /**
     * Check if there are all the incoming connections have been used up
     * @return true if there are still incoming connections available
     */
    public boolean isAnyFreeConnection() {
        if (nConnections >= MAXIMUM_CONNECTIONS) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Add a recently connected peer to the list of peers that this peer currently has
     * @param remoteHostPort
     */
    public void connectedPeer(HostPort remoteHostPort, boolean isIncoming) {
        peers.add(remoteHostPort);

        // Add to the number of incoming connections if it is an incoming connection only
        if (isIncoming) {
            nConnections++;
        }
    }

    public void disconnectPeer(HostPort remoteHostPort) {
        peers.remove(remoteHostPort);
    }

    public ArrayList<HostPort> getPeers(){
        return peers;
    }

    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        // Create runnable here
    }
}
