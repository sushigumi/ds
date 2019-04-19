package unimelb.bitbox.connection;

import unimelb.bitbox.messages.Commands;
import unimelb.bitbox.messages.MessageGenerator;
import unimelb.bitbox.runnables.FileBytesRequest;
import unimelb.bitbox.runnables.FileBytesResponse;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a TCP connection between two peers.
 * Before asynchronous moving on to the asynchronous part of the protocol,
 * connections must either send a HANDSHAKE_REQUEST message and wait for a HANDSHAKE_RESPONSE message
 * or receive a HANDSHAKE_REQUEST and respond with a HANDSHAKE_RESPONSE message
 * If a HANDSHAKE_REQUEST is received but the maximum number of connections has been achieved, then
 * a CONNECTION_REFUSED message must be sent back to the peer and the connection is closed.
 * Similarly, if a CONNECTION_REFUSED message is received, the connection must be closed.
 */
public abstract class Connection {
    Socket socket; // Other peer's socket

    HostPort localHostPort;

    DataOutputStream output;
    DataInputStream input;

    ExecutorService listener;
    ExecutorService sender;
    ExecutorService background;

    FileSystemManager fileSystemManager;

    /**
     * Called when receiving a connection from another peer
     * Only when receiving a connection do we add to the counter of connections based on the
     * maximumIncomingConnections parameter in the properties file
     * @param socket
     * @param localHostPort
     */
    Connection(FileSystemManager fileSystemManager, Socket socket, HostPort localHostPort) {
        this.socket = socket;
        this.localHostPort = localHostPort;
        this.fileSystemManager = fileSystemManager;

        createWriterAndReader();

        this.listener = Executors.newSingleThreadExecutor();

        // Create the single thread executor to send messages based on a queue when it requires messages to be
        // sent
        this.sender = Executors.newSingleThreadExecutor();
        this.background = Executors.newSingleThreadExecutor();
    }

    /**
     * Called when making a connection to another peer
     * So this peer needs to send a handshake request to the other peer
     * @param localHostPort
     */
    Connection(FileSystemManager fileSystemManager, HostPort localHostPort) {
        this.localHostPort = localHostPort;
        this.fileSystemManager = fileSystemManager;

        this.listener = Executors.newSingleThreadExecutor();

        // Create the single thread executor to send messages based on a queue when it requires messages to be
        // sent
        this.sender = Executors.newSingleThreadExecutor();
        this.background = Executors.newSingleThreadExecutor();
    }

    void createWriterAndReader() {
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * Accepts a FileSystemEvent as an argument and constructs an appropriate Runnable to be submitted
     * to the sender ExecutorService
     * @param fileSystemEvent
     */
    public void submitEvent(FileSystemManager.FileSystemEvent fileSystemEvent) {
        switch (fileSystemEvent.event) {
            case FILE_CREATE:
                String request = MessageGenerator.genFileBytesRequests(fileSystemEvent.fileDescriptor.toDoc(), fileSystemEvent.pathName).remove(0);
                // TODO change to sending FILE_CREATE_REQUEST
                sender.submit(new FileBytesResponse(output, fileSystemManager, Document.parse(request)));
                break;
        }

    }

    /**
     * Calls generateSyncEvents method of the File System Manager to synchronize files between all peers
     * at the start of the connection.
     * This method is only called at the start of each connection since further synchronized events will
     * be generated collectively for all other events in the main loop
     */
    void initSyncPeers() {
        ArrayList<FileSystemManager.FileSystemEvent> fileSystemEvents = fileSystemManager.generateSyncEvents();
        // For each file system event send an appropriate message to the connected peer
        for (FileSystemManager.FileSystemEvent event : fileSystemEvents) {
            submitEvent(event);
        }
    }



    class Listener implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    String in = input.readUTF();

                    Document doc = Document.parse(in);

                    Commands command = Commands.valueOf(doc.getString("command"));
                    // TODO switch here

                    System.out.println(in);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
