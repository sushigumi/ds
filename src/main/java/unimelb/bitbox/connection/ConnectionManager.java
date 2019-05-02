package unimelb.bitbox.connection;

import unimelb.bitbox.util.FileSystemManager;

import java.net.Socket;
import java.util.ArrayList;

public class ConnectionManager implements ConnectionObserver {
    private ArrayList<Connection> peers;
    private int nIncomingConnections;


    private static ConnectionManager ourInstance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return ourInstance;
    }

    private ConnectionManager() {
        peers = new ArrayList<>();
        nIncomingConnections = 0;
    }

    /**
     * Connect to a specific peer
     * @param fileSystemManager
     * @param remoteHostPortString
     */
    public void connect(FileSystemManager fileSystemManager, String remoteHostPortString) {
        peers.add(new )
    }

    /**
     * Accept a connection from a peer
     * @param fileSystemManager
     * @param socket
     */
    public void accept(FileSystemManager fileSystemManager, Socket socket) {

    }

    @Override
    public void closeConnection(Connection connection, boolean isIncoming) {

    }

    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {

    }


}
