package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import unimelb.bitbox.threads.ThreadController;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;

	private ServerSocket serverSocket;

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);

		start();
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// TODO: process events
	}

	private void start() {
		// Initialise the socket
		try {
			serverSocket = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue("port")));

			while (true) {
				Socket socket = serverSocket.accept();

				// Create a new thread
				ThreadController.getInstance().newThread(socket);
			}

		} catch (IOException e) {
			// TODO: Send appropriate message
			// Close the server socket if there exists an exception and server is open
			if (serverSocket != null && !serverSocket.isClosed()) {
				try {
					serverSocket.close();
				} catch (IOException f) {
					f.printStackTrace();
				}
			}
			System.out.println("Error! Port is in use. Please use another port or try again.");
			e.printStackTrace();
			// TODO Check if right exit status
			System.exit(-1);
		}
	}
}
