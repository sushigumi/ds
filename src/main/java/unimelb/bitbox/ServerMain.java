package unimelb.bitbox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

	private ThreadPoolExecutor backgroundPool;
	private LinkedBlockingQueue<Runnable> queue;

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);

		this.localHostPort = new HostPort(Configuration.getConfigurationValue("advertisedName"),
										  Integer.parseInt(Configuration.getConfigurationValue("port")));

		start();

		// Initialise the thread pool
		queue = new LinkedBlockingQueue<>();
		backgroundPool = new ThreadPoolExecutor(10, 15, 1000, TimeUnit.SECONDS, queue);
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
			String[] peers = Configuration.getConfigurationValue("peers").split(",");

			ConnectionManager.getInstance().addPeers(queue, peers, localHostPort);

			// Loop to accept incoming connections
			try {
				while (true) {
					Socket socket = serverSocket.accept();

					ConnectionManager.getInstance().addPeer(queue, socket, localHostPort);
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
