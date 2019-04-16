package unimelb.bitbox.connection;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.net.Socket;
import java.util.ArrayList;

/**
 * Handles connections and is a singleton
 */
public class ConnectionManager {
    public final int MAXIMUM_CONNECTIONS;
    private int nConnections;  // number of incoming connections currently active

    private ArrayList<HostPort> peers;

    private static ConnectionManager instance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return instance;
    }

    private ConnectionManager() {
        MAXIMUM_CONNECTIONS = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
        peers = new ArrayList<>();
        nConnections = 0;
    }

    /**
     * Add a peer when making a connection from this peer to another.
     * Local peer is the client
     * @param socket
     * @param localHostPort
     * @param remoteHostPort
     */
    public void addPeer(Socket socket, HostPort localHostPort, HostPort remoteHostPort) {
        Connection connection = new Connection(socket, localHostPort, remoteHostPort);
    }

    /**
     * Add a peer when accepting a connection from another peer
     * Local peer is the server
     * @param socket
     * @param localHostPort
     */
    public void addPeer(Socket socket, HostPort localHostPort) {
        Connection connection = new Connection(socket, localHostPort);
    }

    /**
     * Check if there are all the incoming connections have been used up
     * @return true if there are still incoming connections available
     */
    public boolean isAnyFreeConnection() {
        return nConnections <= MAXIMUM_CONNECTIONS;
    }

    /**
     * Add a recently connected peer to the list of peers that this peer currently has
     * @param peer
     */
    public void connectedPeer(HostPort peer, boolean isIncoming) {
        peers.add(peer);

        // Add to the number of incoming connections if it is an incoming connection only
        if (isIncoming) {
            nConnections++;
        }

        System.out.println(nConnections);
    }

    public void disconnectPeer(HostPort peer) {
        peers.remove(peer);
    }

    public ArrayList<HostPort> getPeers(){
        return peers;
    }

    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {

    }
}
