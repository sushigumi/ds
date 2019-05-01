package unimelb.bitbox.connection;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Handles connections and is a singleton
 */
public class ConnectionManager implements ConnectionObserver {
    private static Logger log = Logger.getLogger(ConnectionManager.class.getName());

    public final int MAXIMUM_CONNECTIONS;
    public final int MAX_RETRIES = 3;
    private int nConnections;  // number of incoming connections currently active

    private ArrayList<Connection> peers;
    private HashMap<String, Integer> retries;

    private static ConnectionManager instance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return instance;
    }

    private ConnectionManager() {
        MAXIMUM_CONNECTIONS = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
        peers = new ArrayList<>();
        retries = new HashMap<>();
        nConnections = 0;
    }

    /**
     * Called to initiate a connection to different peers on start of the server
     * This method is used to allow connections to take place on a separate thread to not block the server
     * while making connections
     */
    public void initiateConnection(FileSystemManager fileSystemManager, String[] hostPorts) {
        for (String hostPort : hostPorts) {
            HostPort remoteHostPort = new HostPort(hostPort);
            retries.put(remoteHostPort.toString(), 0);
            peers.add(new OutgoingConnection(fileSystemManager, this, remoteHostPort));
        }
    }

    /**
     * Called when a connection is accepted from a peer
     * Increments the number of incoming connections
     * @param fileSystemManager
     * @param socket
     */
    public void acceptConnection(FileSystemManager fileSystemManager, Socket socket) {
        peers.add(new IncomingConnection(fileSystemManager, socket, this));
        nConnections++;
    }

    @Override
    public void updateRetries(HostPort remoteHostPort) {
        if (retries.containsKey(remoteHostPort.toString())) {
            retries.put(remoteHostPort.toString(), retries.get(remoteHostPort.toString()) + 1);
        } else {
            retries.put(remoteHostPort.toString(), 0);
        }
    }

    /**
     * Get a list of host ports of peers that are currently connected
     * @return
     */
    public ArrayList<HostPort> getPeersHostPort(HostPort currentRemoteHostPort) {
        ArrayList<HostPort> hostPorts = new ArrayList<>();
        for (Connection peer : peers) {
            if (peer.remoteHostPort != null && !peer.remoteHostPort.equals(currentRemoteHostPort)) {
                hostPorts.add(peer.remoteHostPort);
            }
        }

        return hostPorts;
    }

    /**
     * Called when a FileSystemEvent is received on the Server Main and propagates the file system event
     * too all peers
     * @param fileSystemEvent
     */
    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        for (Connection peer: peers) {
            peer.submitEvent(fileSystemEvent);
        }
    }

    /**
     * A connection has been closed and remove the peer from peers list by comparing it
     * based on its pointer
     * @param connection
     * @param isIncoming
     */
    @Override
    public void closeConnection(Connection connection, boolean isIncoming) {
        if (isIncoming) {
            nConnections--;
        }

        peers.remove(connection);
        log.info("connection has been closed");
        log.info(peers.size() + " peers currently connected");
    }

    /**
     * Called when this peer receives an invalid protocol and attempts to reconnect to the peer who
     * closed the connection
     * @param fileSystemManager
     * @param remoteHostPort
     */
    @Override
    public void retry(FileSystemManager fileSystemManager, HostPort remoteHostPort) {
        if (retries.get(remoteHostPort.toString()) < MAX_RETRIES) {
            peers.add(new OutgoingConnection(fileSystemManager, this, remoteHostPort));
            retries.put(remoteHostPort.toString(), retries.get(remoteHostPort.toString()) + 1);
        } else {
            retries.remove(remoteHostPort.toString());
        }
    }

    /**
     * True if there are still available slots for incoming connection
     * @return
     */
    public boolean isAnyFreeConnection() {
        return nConnections <= MAXIMUM_CONNECTIONS;
    }
}
