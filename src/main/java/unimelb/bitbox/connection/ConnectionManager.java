package unimelb.bitbox.connection;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Handles connections and is a singleton
 */
public class ConnectionManager implements ConnectionObserver {
    public final int MAXIMUM_CONNECTIONS;
    private int nConnections;  // number of incoming connections currently active

    private ArrayList<HostPort> peerHostPorts;
    private ArrayList<Connection> peers;

    private HashMap<HostPort, Integer> retries;

    private static ConnectionManager instance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return instance;
    }

    private ConnectionManager() {
        MAXIMUM_CONNECTIONS = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
        peerHostPorts = new ArrayList<>();
        peers = new ArrayList<>();
        retries = new HashMap<>();
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

            retries.put(remoteHostPort, 0);

        }
    }

    /**
     * Add a peer when making a connection from this peer to another.
     * Local peer is the client
     * @param localHostPort
     * @param remoteHostPort
     */
    public void addPeer(FileSystemManager fileSystemManager, HostPort localHostPort, HostPort remoteHostPort) {
        Connection connection = new OutgoingConnection(fileSystemManager, localHostPort, remoteHostPort);
        connection.addConnectionObserver(this);
        peers.add(connection);

        retries.put(remoteHostPort, 0);

    }

    /**
     * Add a peer when accepting a connection from another peer
     * Local peer is the server
     * @param socket
     * @param localHostPort
     */
    public void addPeer(FileSystemManager fileSystemManager, Socket socket, HostPort localHostPort) {
        Connection connection = new IncomingConnection(fileSystemManager, socket, localHostPort);
        connection.addConnectionObserver(this);
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

    public ArrayList<HostPort> getPeers(){
        return peerHostPorts;
    }

    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        // Create runnable here
        for (Connection connection: peers) {
            connection.submitEvent(fileSystemEvent);
        }
    }

    @Override
    public void closeConnection(HostPort remoteHostPort, boolean isIncoming) {
        // Reduce the number of connections
        if (isIncoming) {
            nConnections--;
        }

        // Get the index of the connection
        peerHostPorts.remove(remoteHostPort);

        // Remove connection
        Iterator<Connection> i = peers.iterator();
        while (i.hasNext()) {
            Connection s = i.next(); // must be called before you can call i.remove()
            // Do something
            if (s.remoteHostPort == null) {
                i.remove();
            }
            else if (s.remoteHostPort.equals(remoteHostPort)) {
                i.remove();
            }
        }
    }

    @Override
    public void interruptConnection(HostPort remoteHostPort, HostPort localHostPort, FileSystemManager fileSystemManager) {
        // Retry the connection if retries is less than 3
        if (retries.get(remoteHostPort) < 3) {
            Connection connection = new OutgoingConnection(fileSystemManager, localHostPort, remoteHostPort);
            connection.addConnectionObserver(this);
            peers.add(connection);
        } else{
            retries.remove(remoteHostPort);
        }
    }
}
