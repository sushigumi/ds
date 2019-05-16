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

import unimelb.bitbox.eventprocess.*;
import unimelb.bitbox.messages.InvalidProtocolType;
import unimelb.bitbox.messages.MessageValidator;
import unimelb.bitbox.messages.Messages;
import unimelb.bitbox.peer.TCPPeerManager;
import unimelb.bitbox.peer.UDPPeerManager;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	public static final String MODE_UDP = "udp";
	public static String MODE_TCP = "tcp";

	private String mode; // TODO could change to enum?

	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;

	private static HostPort localHostPort = null;

	private ScheduledExecutorService timer;

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);

		// Set the mode
		mode = Configuration.getConfigurationValue("mode");

		// Set the local host ports and start the appropriate server
		if (mode.equals(MODE_TCP)) {
			setLocalHostPort(Integer.parseInt(Configuration.getConfigurationValue("port")));
			new TCPServerThread().start();
		} else if (mode.equals(MODE_UDP)) {
			setLocalHostPort(Integer.parseInt(Configuration.getConfigurationValue("udpPort")));
			new UDPServerThread().start();
		}
		// Invalid configuration
		else {
			log.severe("invalid server mode. please recheck configuration properties");
			System.exit(1);
		}
	}

	/**
	 * Set the local host port
	 * @param port
	 */
	private void setLocalHostPort(int port) {
		localHostPort = new HostPort(Configuration.getConfigurationValue("advertisedName"), port);
	}

	/**
	 * Get the local host port
	 * @return
	 */
	public static HostPort getLocalHostPort() {
		return localHostPort;
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
		private DatagramSocket serverSocket;
		private int blockSize;

		private ExecutorService backgroundExecutor;

		UDPServerThread() throws IOException {
			super("UDPServer");

			// Start the server with the appropriate port number
			int portNumber = Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
			serverSocket = new DatagramSocket(portNumber);

			// Get the blocksize of datagram packet
			blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));

			// Set up a background thread to handle whatever incoming request received
			this.backgroundExecutor = Executors.newSingleThreadExecutor();
		}

		/**
		 * Listen for incoming packets from the client
		 */
		@Override
		public void run() {
			// Send a HANDSHAKE_REQUEST to all the peers in the configuration file
			// Connect to all the peers first
			String[] peers = Configuration.getConfigurationValue("peers").split(",");
			for (String peer : peers) {
				UDPPeerManager.getInstance().addPeer(serverSocket, peer);
			}

			// Listen for any messages
			while (true) {
				try {
					// Create a new buffer to accept requests
					byte[] buf = new byte[blockSize];

					// Receive the request
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					serverSocket.receive(packet);

					// Get the message from the packet in UTF-8 format
					String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
					HostPort remoteHostPort = new HostPort(packet.getAddress().getHostAddress(), packet.getPort());
					System.out.println(packet.getAddress());
					System.out.println(packet.getSocketAddress());
					System.out.println(message);

					//TODO remoteHostPort checking to ensure that nothing received from hostport which has been disconnected

					processMessage(message, remoteHostPort);
				}
				// Caught exception so exit and log to user
				catch (IOException e) {
					e.printStackTrace();
					log.severe("error occurred when accepting a packet");
					serverSocket.close();
					return;
				}
			}
		}

		/**
t		 * Helper method to submit the appropriate runnable to the background thread.
		 * @param message
		 */
		private void processMessage(String message, HostPort remoteHostPort) {
			// Peer has closed the peer and EOF received
			if (message == null) {
				log.info("peer is disconnected");
				UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				return;
			}

			Document doc = Document.parse(message);
			String command = doc.getString("command");

			//System.out.println("Received: " + command);  // Debugging async

			// Received INVALID_PROTOCOL, close the peer
			if (command.equals(Messages.INVALID_PROTOCOL)) {
				UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
			}
			else if (command.equals(Messages.FILE_CREATE_REQUEST)) {
				String createRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
				if (createRequest != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
				else {
					backgroundExecutor.submit(new FileCreateResponse(serverSocket, remoteHostPort, doc, fileSystemManager));
				}
			}
			else if (command.equals(Messages.FILE_CREATE_RESPONSE)) {
				String createResponse = MessageValidator.getInstance().validateFileChangeResponse(doc);
				if (createResponse != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, createResponse));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
			}
			else if (command.equals(Messages.FILE_DELETE_REQUEST)) {
				String deleteRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
				if (deleteRequest != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, deleteRequest));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
				else {
					backgroundExecutor.submit(new FileDeleteResponse(serverSocket, remoteHostPort, doc, fileSystemManager));
				}
			}
			else if (command.equals(Messages.FILE_DELETE_RESPONSE)) {
				String deleteResponse = MessageValidator.getInstance().validateFileChangeResponse(doc);
				if (deleteResponse != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, deleteResponse));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
			}
			else if (command.equals(Messages.FILE_MODIFY_REQUEST)) {
				String modifyRequest = MessageValidator.getInstance().validateFileChangeRequest(doc);
				if (modifyRequest != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, modifyRequest));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
				else {
					backgroundExecutor.submit(new FileModifyResponse(serverSocket, remoteHostPort, doc, fileSystemManager));
				}
			}
			else if (command.equals(Messages.FILE_MODIFY_RESPONSE)) {
				String modifyResponse = MessageValidator.getInstance().validateFileChangeResponse(doc);
				if (modifyResponse != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, modifyResponse));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
			}
			else if (command.equals(Messages.FILE_BYTES_REQUEST)) {
				String bytesRequest = MessageValidator.getInstance().validateFileBytesRequest(doc);
				if (bytesRequest != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, bytesRequest));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
				else {
					backgroundExecutor.submit(new FileBytesResponse(serverSocket, remoteHostPort, fileSystemManager, doc));
				}
			}
			else if (command.equals(Messages.FILE_BYTES_RESPONSE)) {
				String bytesResponse = MessageValidator.getInstance().validateFileBytesResponse(doc);
				if (bytesResponse != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, bytesResponse));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
				else {
					backgroundExecutor.submit(new ConstructFile(serverSocket, remoteHostPort, fileSystemManager, doc));
				}
			}
			else if (command.equals(Messages.DIRECTORY_CREATE_REQUEST)) {
				String dirCreateRequest = MessageValidator.getInstance().validateDirectoryChangeRequest(doc);
				if (dirCreateRequest != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, dirCreateRequest));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
				else {
					backgroundExecutor.submit(new DirectoryCreateResponse(serverSocket, remoteHostPort, fileSystemManager, doc));
				}
			}
			else if (command.equals(Messages.DIRECTORY_CREATE_RESPONSE)) {
				String dirCteateResponse = MessageValidator.getInstance().validateDirectoryChangeResponse(doc);
				if (dirCteateResponse != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, dirCteateResponse));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
			}
			else if (command.equals(Messages.DIRECTORY_DELETE_REQUEST)) {
				String dirDeleteRequest = MessageValidator.getInstance().validateDirectoryChangeRequest(doc);
				if (dirDeleteRequest != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, dirDeleteRequest));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
				else {
					backgroundExecutor.submit(new DirectoryDeleteResponse(serverSocket, remoteHostPort, fileSystemManager, doc));
				}
			}
			else if (command.equals(Messages.DIRECTORY_DELETE_RESPONSE)) {
				String dirDeleteResponse = MessageValidator.getInstance().validateDirectoryChangeResponse(doc);
				if (dirDeleteResponse != null) {
					backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.MISSING_FIELD, dirDeleteResponse));
					UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
				}
			}
			else {
				backgroundExecutor.submit(new InvalidProtocol(serverSocket, remoteHostPort, InvalidProtocolType.INVALID_COMMAND));
				UDPPeerManager.getInstance().disconnectPeer(remoteHostPort);
			}
		}
	}
}
