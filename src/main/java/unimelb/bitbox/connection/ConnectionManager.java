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

    private ArrayList<HostPort> peerHostPorts;
    private ArrayList<Connection> peers;

    private static ConnectionManager instance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return instance;
    }

    private ConnectionManager() {
        MAXIMUM_CONNECTIONS = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
        peerHostPorts = new ArrayList<>();
        peers = new ArrayList<>();
        nConnections = 0;
    }

    /**
     * For each peer in peers calls the method addPeer to set up a connection to each of the
     * peers
     * @param peers
     */
    public void addPeers(FileSystemManager fileSystemManager, String[] peers, HostPort localHostPort) {
        for (String peer : peers) {
            HostPort remoteHostPort = new HostPort(peer);

            addPeer(fileSystemManager, localHostPort, remoteHostPort);

        }
    }

    /**
     * Add a peer when making a connection from this peer to another.
     * Local peer is the client
     * @param queue
     * @param localHostPort
     * @param remoteHostPort
     */
    public void addPeer(FileSystemManager fileSystemManager, HostPort localHostPort, HostPort remoteHostPort) {
        Connection connection = new OutgoingConnection(fileSystemManager, localHostPort, remoteHostPort);
        peers.add(connection);

    }

    /**
     * Add a peer when accepting a connection from another peer
     * Local peer is the server
     * @param socket
     * @param localHostPort
     */
    public void addPeer(FileSystemManager fileSystemManager, Socket socket, HostPort localHostPort) {
        Connection connection = new IncomingConnection(fileSystemManager, socket, localHostPort);
        peers.add(connection);
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
        peerHostPorts.add(remoteHostPort);

        // Add to the number of incoming connections if it is an incoming connection only
        if (isIncoming) {
            nConnections++;
        }
    }

    // TODO need to disconnect the connection Arraylist as well
    public void disconnectPeer(HostPort remoteHostPort) {
        peerHostPorts.remove(remoteHostPort);
    }

    public ArrayList<HostPort> getPeers(){
        return peerHostPorts;
    }

    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        // Create runnable here

        /*
        switch (fileSystemEvent.event){

            case DIRECTORY_DELETE:
                new DirectoryDeleteRequest(output, );

        }
        */

    }
}
