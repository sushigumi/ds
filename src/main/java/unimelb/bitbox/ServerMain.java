package unimelb.bitbox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.*;
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

	public static final HostPort localHostPort = new HostPort(Configuration.getConfigurationValue("advertisedName"),
															  Integer.parseInt(Configuration.getConfigurationValue("port")));

	private ScheduledExecutorService timer;

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);

		start();
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// TODO: process events
		//Make a new thread
		ConnectionManager.getInstance().processFileSystemEvent(fileSystemEvent);
	}

	/**
	 * Start the server
	 */
	private void start() {
		// Generate a timer to peridocially sync events to peers
		Runnable periodicSync = new Runnable() {
			@Override
			public void run() {
				ArrayList<FileSystemEvent> fileSystemEvents = fileSystemManager.generateSyncEvents();

				for (FileSystemEvent event : fileSystemEvents) {
					ConnectionManager.getInstance().processFileSystemEvent(event);
				}
			}
		};

		timer = Executors.newSingleThreadScheduledExecutor();
		long syncInterval = Long.parseLong(Configuration.getConfigurationValue("syncInterval"));
		timer.scheduleAtFixedRate(periodicSync, syncInterval, syncInterval, TimeUnit.SECONDS);

		// Create a server socket
		try {
			ServerSocket serverSocket = new ServerSocket(localHostPort.port);

			// Connect to the peers
			String[] peers = Configuration.getConfigurationValue("peers").split(",");
			ConnectionManager.getInstance().initiateConnection(fileSystemManager, peers);

			// Loop to accept incoming connections
			try {
				while (true) {
					Socket socket = serverSocket.accept();

					ConnectionManager.getInstance().acceptConnection(fileSystemManager, socket);
				}
			} catch (IOException e) {
				log.severe("Error has occurred. Please restart the server");
				e.printStackTrace();
			}
		} catch (IOException e) {
			log.severe("Failed to start server");
			e.printStackTrace();
			System.exit(1);
		}

	}
}
