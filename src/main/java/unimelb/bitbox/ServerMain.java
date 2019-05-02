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

	}

	/**
	 * Start the server
	 */
	private void start() {
		// Generate a timer to periodically sync events to peers
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
		timer.scheduleAtFixedRate(periodicSync, 0, syncInterval, TimeUnit.SECONDS);

		// Connect to all the peers first
		String[] peers = Configuration.getConfigurationValue("peers").split(",");
		for (String peer : peers) {

		}

		// Start the server
		int portNumber = Integer.parseInt(Configuration.getConfigurationValue("port"));
		try {
			ServerSocket serverSocket = new ServerSocket(portNumber);
			while (true) {
				try {
					Socket socket = serverSocket.accept();
				} catch (IOException e) {
					log.severe("error occurred when accepting connection");
				}
			}

		} catch (IOException e) {
			log.severe("unable to start server. exiting");
			System.exit(1);
		}
	}
}
