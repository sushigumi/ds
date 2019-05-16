package unimelb.bitbox;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.logging.Logger;

import unimelb.bitbox.peer.TCPPeerManager;
import unimelb.bitbox.peer.UDPPeerManager;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

public class ServerMain implements FileSystemObserver {
	public static final String MODE_UDP = "udp";
	public static String MODE_TCP = "tcp";

	private String mode; // TODO could change to enum?

	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;

	public static final HostPort localHostPort = new HostPort(Configuration.getConfigurationValue("advertisedName"),
			Integer.parseInt(Configuration.getConfigurationValue("port")));

	private ScheduledExecutorService timer;

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);

		mode = Configuration.getConfigurationValue("mode");
		if (mode.equals(MODE_TCP)) {
			new TCPServerThread().start();
		} else if (mode.equals(MODE_UDP)) {
			new UDPServerThread().start();
		}
		// Invalid configuration
		else {
			log.severe("invalid server mode. please recheck configuration properties");
			System.exit(1);
		}
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		if (mode.equals(MODE_TCP)) {
			TCPPeerManager.getInstance().processFileSystemEvent(fileSystemEvent);
		}
		else if (mode.equals(MODE_UDP)) {
			UDPPeerManager.getInstance().processFileSystemEvent(fileSystemEvent);
		}
	}

	/**
	 * Start a TCP server
	 */
	private class TCPServerThread extends Thread {
		private ServerSocket serverSocket;

		TCPServerThread() throws IOException {
			// Give it a name
			super("TCPServer");

			// Start the server
			int portNumber = Integer.parseInt(Configuration.getConfigurationValue("port"));
			serverSocket = new ServerSocket(portNumber);
		}

		@Override
		public void run() {
			// Generate a timer to periodically sync events to peers
			Runnable periodicSync = new Runnable() {
				@Override
				public void run() {
					ArrayList<FileSystemEvent> fileSystemEvents = fileSystemManager.generateSyncEvents();

					for (FileSystemEvent event : fileSystemEvents) {
						TCPPeerManager.getInstance().processFileSystemEvent(event);
					}
				}
			};

			timer = Executors.newSingleThreadScheduledExecutor();
			long syncInterval = Long.parseLong(Configuration.getConfigurationValue("syncInterval"));
			timer.scheduleAtFixedRate(periodicSync, syncInterval, syncInterval, TimeUnit.SECONDS);

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

	/**
	 * Start a UDP Server thread
	 */
	private class UDPServerThread extends Thread {
		private DatagramSocket socket;
		private int blockSize;

		UDPServerThread() throws IOException {
			super("UDPServer");

			// Start the server with the appropriate port number
			int portNumber = Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
			socket = new DatagramSocket(portNumber);

			// Get the blocksize of datagram packet
			blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
		}

		/**
		 * Listen for incoming packets from the client
		 */
		@Override
		public void run() {
			while (true) {
				try {
					// Create a new buffer to accept requests
					byte[] buf = new byte[blockSize];

					// Receive the request
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);

					// Get the message from the packet in UTF-8 format
					String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
					System.out.println(message);
				}
				// Caught exception so exit and log to user
				catch (IOException e) {
					e.printStackTrace();
					log.severe("error occurred when accepting a packet");
					socket.close();
					return;
				}
			}
		}
	}
}
