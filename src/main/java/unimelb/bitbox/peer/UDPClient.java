package unimelb.bitbox.peer;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.eventprocess.*;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//TODO add retry mechanism
public class UDPClient {
    private ExecutorService sender;

    private DatagramSocket serverSocket;
    private HostPort remoteHostPort;

    public UDPClient(DatagramSocket serverSocket, HostPort remoteHostPort) {
        this.serverSocket = serverSocket;
        this.remoteHostPort = remoteHostPort;
        sender = Executors.newSingleThreadExecutor();

        // Start the handshake process
        connect();
    }

    /**
     * Start a handshake, sends a HANDSHAKE_REQUEST to the specified peer to request for a connection. If the connection
     * is refused, then tries to connect with all the others returned from the CONNECTION_REFUSED message.
     */
    private void connect() {
        // Send a HANDSHAKE_REQUEST to the peer
        sender.submit(new Handshake(serverSocket, remoteHostPort));
    }

    // TODO disconnect
    private void disconnect() {

    }

    public void processFileSystemEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        switch(fileSystemEvent.event) {
            case FILE_CREATE:
                sender.submit(new FileCreateRequest(serverSocket, remoteHostPort, fileSystemEvent));
                break;

            case FILE_DELETE:
                sender.submit(new FileDeleteRequest(serverSocket, remoteHostPort, fileSystemEvent));
                break;

            case FILE_MODIFY:
                sender.submit(new FileModifyRequest(serverSocket, remoteHostPort, fileSystemEvent));
                break;

            case DIRECTORY_CREATE:
                sender.submit(new DirectoryCreateRequest(serverSocket, remoteHostPort, fileSystemEvent.pathName));
                break;

            case DIRECTORY_DELETE:
                sender.submit(new DirectoryDeleteRequest(serverSocket, remoteHostPort, fileSystemEvent.pathName));
                break;
        }
    }

    /**
     * Get the host port of the client
     * @return
     */
    public HostPort getRemoteHostPort() {
        return remoteHostPort;
    }

    /**
     * Send a handshake request to the peer
     */
    private class Handshake extends EventProcess {
        public Handshake(DatagramSocket socket, HostPort hostPort) {
            super(socket, hostPort);
        }

        @Override
        public void run() {
            sendMessage(Messages.genHandshakeRequest(ServerMain.getLocalHostPort()));
        }
    }
}
