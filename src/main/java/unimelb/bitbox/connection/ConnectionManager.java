package unimelb.bitbox.connection;

import unimelb.bitbox.util.HostPort;

import java.net.Socket;

public class ConnectionManager {

    /**
     * Add a peer when making a connection from this peer to another.
     * Local peer is the client
     * @param socket
     * @param localHostPort
     * @param remoteHostPort
     */
    public static void addPeer(Socket socket, HostPort localHostPort, HostPort remoteHostPort) {
        Connection connection = new Connection(socket, localHostPort, remoteHostPort);
    }

    /**
     * Add a peer when accepting a connection from another peer
     * Local peer is the server
     * @param socket
     * @param localHostPort
     */
    public static void addPeer(Socket socket, HostPort localHostPort) {
        Connection connection = new Connection(socket, localHostPort);
    }
}
