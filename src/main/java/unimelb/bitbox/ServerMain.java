package unimelb.bitbox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import unimelb.bitbox.connection.ConnectionManager;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;

	private HostPort localHostPort;

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);

		this.localHostPort = new HostPort(Configuration.getConfigurationValue("advertisedName"),
										  Integer.parseInt(Configuration.getConfigurationValue("port")));

		start();
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// TODO: process events
		//Make a new thread
	}

	/**
	 * Start the server
	 */
	private void start() {
		// Create a server socket
		try {
			ServerSocket serverSocket = new ServerSocket(localHostPort.port);

			// Connect to the peers
			String peers[] = Configuration.getConfigurationValue("peers").split(",");

			for (String peer : peers) {
				HostPort remoteHostPort = new HostPort(peer);

				// Make a new socket to connect
				try {
					Socket socket = new Socket(remoteHostPort.host, remoteHostPort.port);

					 ConnectionManager.addPeer(socket, localHostPort, remoteHostPort);
				} catch (IOException e) {
					log.info("Unable to connect to " + peer + ". Peer could be offline");
				}

			}


			// Loop to accept incoming connections
			try {
				while (true) {
					Socket socket = serverSocket.accept();

					ConnectionManager.addPeer(socket, localHostPort);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			log.info("Failed to start server");
			e.printStackTrace();
			System.exit(1);
		}

	}
}
