package unimelb.bitbox;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import unimelb.bitbox.peer.TCPPeerManager;
import unimelb.bitbox.peer.UDPPeerManager;
import unimelb.bitbox.server.Server;
import unimelb.bitbox.server.TCPServerThread;
import unimelb.bitbox.server.UDPServerThread;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	public static final String MODE_UDP = "udp";
	public static final String MODE_TCP = "tcp";

	public static final int MAX_RETRIES = Integer.parseInt(Configuration.getConfigurationValue("udpRetries"));

	private static String mode; // TODO could change to enum?

	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;

	private TCPServerThread tcp = null;
	private UDPServerThread udp = null;

	private static HostPort localHostPort = null;

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);

		// Set the mode
		mode = Configuration.getConfigurationValue("mode");

		// Set the local host ports and start the appropriate server
		if (mode.equals(MODE_TCP)) {
			setLocalHostPort(Integer.parseInt(Configuration.getConfigurationValue("port")));
			tcp = new TCPServerThread(fileSystemManager);
			tcp.start();
			new Server(fileSystemManager);
		} else if (mode.equals(MODE_UDP)) {
			setLocalHostPort(Integer.parseInt(Configuration.getConfigurationValue("udpPort")));
			udp = new UDPServerThread(fileSystemManager);
			udp.start();
			new Server(udp.getServerSocket(),fileSystemManager);
		}

		// Invalid configuration
		else {
			log.severe("invalid server mode. please recheck configuration properties");
			System.exit(1);
		}

	}

	/**
	 * Get the mode of the server
	 * @return
	 */
	public static String getMode() {
		return mode;
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
}
