package unimelb.bitbox.server;


import unimelb.bitbox.peer.TCPPeerManager;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Start a TCP server
 */
public class TCPServerThread extends ServerThread {
    private static Logger log = Logger.getLogger(TCPServerThread.class.getName());
    private ServerSocket serverSocket;

    public TCPServerThread(FileSystemManager fileSystemManager) throws IOException {
        // Give it a name
        super("TCPServer", fileSystemManager);

        // Start the server
        int portNumber = Integer.parseInt(Configuration.getConfigurationValue("port"));
        serverSocket = new ServerSocket(portNumber);
    }

    @Override
    public void run() {
        // Connect to all the peers first
        String[] peers = Configuration.getConfigurationValue("peers").split(",");
        for (String peer : peers) {
            TCPPeerManager.getInstance().connect(fileSystemManager, peer);
        }

        // Loop and receive connections
        while (true) {
            try {
                Socket socket = serverSocket.accept();

                TCPPeerManager.getInstance().accept(fileSystemManager, socket);
            } catch (IOException e) {
                log.severe("error occurred when accepting peer");
            }
        }
    }
}